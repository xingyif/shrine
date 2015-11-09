package net.shrine.broadcaster

import net.shrine.log.Loggable
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.DeleteQueryRequest
import org.junit.Test
import net.shrine.protocol.NodeId
import net.shrine.protocol.Result
import scala.concurrent.Future
import net.shrine.adapter.client.AdapterClient
import net.shrine.protocol.DeleteQueryResponse
import net.shrine.util.ShouldMatchersForJUnit
import scala.concurrent.Await
import net.shrine.dao.squeryl.SquerylEntryPoint
import net.shrine.protocol.RunQueryRequest
import net.shrine.protocol.ResultOutputType
import net.shrine.protocol.query.Term
import net.shrine.protocol.query.Or
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.RunQueryResponse
import net.shrine.protocol.QueryResult
import net.shrine.util.XmlDateHelper
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.ErrorResponse
import net.shrine.broadcaster.dao.model.HubQueryResultRow
import net.shrine.broadcaster.dao.model.HubQueryStatus

/**
 * @author clint
 * @since Dec 15, 2014
 */
//noinspection UnitMethodIsParameterless,NameBooleanParameters,ScalaUnnecessaryParentheses
final class AdapterClientBroadcasterLoggingTest extends AbstractSquerylHubDaoTest with ShouldMatchersForJUnit with Loggable {

  private def makeBroadcaster(nodes: Set[NodeHandle]): AdapterClientBroadcaster = AdapterClientBroadcaster(nodes, dao)

  import scala.concurrent.ExecutionContext.Implicits.global
  
  import scala.concurrent.duration._
  
  private final class TestAdapterClient(toReturn: => Result) extends AdapterClient {
    override def query(message: BroadcastMessage): Future[Result] = Future { toReturn }
  }
  
  private object TestAdapterClient {
    def apply(toReturn: => Result): TestAdapterClient = new TestAdapterClient(toReturn)
  }
  
  private val authn = AuthenticationInfo("domain", "username", Credential("asdasd", false))
  
  private val queryDef = QueryDefinition("foo", Or(Term("x"), Term("y")))
  
  private val broadcastMessageDelete = {
    BroadcastMessage(authn, DeleteQueryRequest("projectId", 12345.milliseconds, authn, 12345L))
  }
  
  import ResultOutputType.PATIENT_COUNT_XML
  
  private val broadcastMessageRunQuery = {
    BroadcastMessage(authn, RunQueryRequest("projectId", 12345.milliseconds, authn, None, None, Set(PATIENT_COUNT_XML), queryDef))
  }

  import SquerylEntryPoint._
  
  @Test
  def testShouldntLogAllNodesSucceed: Unit = afterCreatingTables {
    val workingNodes = Set(
        NodeHandle(NodeId("X"), TestAdapterClient(Result(NodeId("X"), 1.second, DeleteQueryResponse(12345L)))),
        NodeHandle(NodeId("Y"), TestAdapterClient(Result(NodeId("Y"), 1.second, DeleteQueryResponse(12345L)))))
        
    doTestShouldntLog(workingNodes)
  }
  
  @Test
  def testShouldntLogSomeNodesSucceed: Unit = afterCreatingTables {
    val nodes = Set(
        NodeHandle(NodeId("X"), TestAdapterClient(Result(NodeId("X"), 1.second, DeleteQueryResponse(12345L)))),
        NodeHandle(NodeId("Y"), TestAdapterClient(throw new Exception)))
        
    doTestShouldntLog(nodes)
  }
  
  @Test
  def testShouldntLogNoNodesSucceed: Unit = afterCreatingTables {
    val failingNodes = Set(
        NodeHandle(NodeId("X"), TestAdapterClient(throw new Exception)),
        NodeHandle(NodeId("Y"), TestAdapterClient(throw new Exception)))
        
    doTestShouldntLog(failingNodes, classOf[ErrorResponse])
  }
  
  private def doTestShouldntLog(nodeHandles: Set[NodeHandle], expectedAggregatedResponseType: Class[_ <: ShrineResponse] = classOf[DeleteQueryResponse]): Unit = {
    val broadcaster = makeBroadcaster(nodeHandles)
    
    val responses = Await.result(broadcaster.broadcast(broadcastMessageDelete).responses, Duration.Inf)
    
    responses.size should equal(nodeHandles.size)
    
    //Only log RunQueryRequests
    list(queryRows) should be(Nil)
    
    //Only log RunQueryRequests
    list(queryResultRows) should be(Nil)
  }
  
  import net.shrine.broadcaster.dao.model.{HubQueryStatus => hqs}
  
  @Test
  def testShouldLogAllNodesSucceed: Unit = doTestShouldLog(Map("X" -> hqs.Success, "Y" -> hqs.Success))
  
  @Test
  def testShouldLogSomeNodesSucceed: Unit = doTestShouldLog(Map("X" -> hqs.Success, "Y" -> hqs.Failure))
  
  @Test
  def testShouldLogNoNodesSucceed: Unit = doTestShouldLog(Map("X" -> hqs.Failure, "Y" -> hqs.Failure))
  
  private def doTestShouldLog(expectedStatusesByNodeName: Map[String, HubQueryStatus]): Unit = afterCreatingTables {
    val queryResult = QueryResult(99L, 11L,Some(PATIENT_COUNT_XML), 42L, Some(XmlDateHelper.now), Some(XmlDateHelper.now), Some("desc"), QueryResult.StatusType.Finished, Some("status"))
    
    val nodes: Set[NodeHandle] = expectedStatusesByNodeName.map { case (nodeName, status) => 
      val adapterClient = status match {
        case hqs.Success => TestAdapterClient(Result(NodeId(nodeName), 1.second, RunQueryResponse(12345L, XmlDateHelper.now, "uid", "gid", queryDef, 42L, queryResult)))
        case hqs.Failure => TestAdapterClient(throw new Exception)
      }
      
      NodeHandle(NodeId(nodeName), adapterClient)
    }.toSet
        
    val broadcaster = makeBroadcaster(nodes)
    
    val responses = Await.result(broadcaster.broadcast(broadcastMessageRunQuery).responses, Duration.Inf)
    
    responses.size should equal(expectedStatusesByNodeName.size)
    
    val Seq(queryRow) = list(queryRows).map(_.toHubQueryRow)
    
    queryRow.networkQueryId should be(broadcastMessageRunQuery.requestId)
    queryRow.queryDefinition should be(queryDef)
    queryRow.domain should be(authn.domain)
    queryRow.username should be(authn.username)
    queryRow.time should not be(null)

    val resultRows: Seq[HubQueryResultRow] = list(queryResultRows).map(_.toHubQueryResultRow)

    debug(s"resultRows are $resultRows")
    resultRows.size should be(2)

    val resultRow1 = resultRows.head
    val resultRow2 = resultRows.tail.head

    resultRow1.networkQueryId should be(broadcastMessageRunQuery.requestId)
    resultRow2.networkQueryId should be(broadcastMessageRunQuery.requestId)
    
    val resultRowsByNode: Map[String, HubQueryResultRow] = resultRows.map(row => row.nodeName -> row).toMap
    
    resultRowsByNode.keySet should be(expectedStatusesByNodeName.keySet)
    
    for {
      (nodeName, expectedStatus) <- expectedStatusesByNodeName
    } {
      resultRowsByNode(nodeName).status should be(expectedStatus)
    }
    
    resultRow1.timestamp should not be(null)
    resultRow2.timestamp should not be(null)
  }
}