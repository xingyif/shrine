package net.shrine.adapter.audit

import java.sql.SQLException
import javax.sql.DataSource

import com.typesafe.config.Config
import net.shrine.audit.{NetworkQueryId, QueryName, QueryTopicId, QueryTopicName, ShrineNodeId, Time, UserName}
import net.shrine.crypto.KeyStoreEntry
import net.shrine.log.Loggable
import net.shrine.protocol.{BroadcastMessage, RunQueryRequest, RunQueryResponse, ShrineResponse}
import net.shrine.slick.TestableDataSourceCreator
import net.shrine.source.ConfigSource
import slick.driver.JdbcProfile

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future, blocking}
import scala.language.postfixOps

/**
 * DB code for the Adapter audit metrics.
 *
 * @author david
 * @since 8/25/15
 */
case class AdapterAuditDb(schemaDef:AdapterAuditSchema,dataSource: DataSource) extends Loggable {
  import schemaDef._
  import jdbcProfile.api._

  val database = Database.forDataSource(dataSource)

  def createTables() = schemaDef.createTables(database)

  def dropTables() = schemaDef.dropTables(database)

  def dbRun[R](action: DBIOAction[R, NoStream, Nothing]):R = {
    val future: Future[R] = database.run(action)
    blocking {
      Await.result(future, 10 seconds)
    }
  }

  def insertQueryReceived(broadcastMessage: BroadcastMessage):Unit = {
    debug(s"insertQueryReceived $broadcastMessage")

    QueryReceived.fromBroadcastMessage(broadcastMessage).foreach(insertQueryReceived)
  }

  def insertQueryReceived(queryReceived:QueryReceived):Unit = {
    dbRun(allQueriesReceived += queryReceived)
  }

  def selectAllQueriesReceived:Seq[QueryReceived] = {
    dbRun(allQueriesReceived.result)
  }

  def insertExecutionStarted(runQueryRequest: RunQueryRequest):Unit = {
    debug(s"insertExecutionStarted $runQueryRequest")

    insertExecutionStarted(ExecutionStarted.fromRequest(runQueryRequest))
  }

  def insertExecutionStarted(executionStart:ExecutionStarted):Unit = {
    dbRun(allExecutionsStarted += executionStart)
  }

  def selectAllExecutionStarts:Seq[ExecutionStarted] = {
    dbRun(allExecutionsStarted.result)
  }

  def insertExecutionCompletedShrineResponse(request: RunQueryRequest,shrineResponse: ShrineResponse) = {
    debug(s"insertExecutionCompleted $shrineResponse for $request")

    ExecutionCompleted.fromRequestResponse(request,shrineResponse).foreach(insertExecutionCompleted)
  }

  def insertExecutionCompleted(executionCompleted:ExecutionCompleted):Unit = {
    dbRun(allExecutionsCompleted += executionCompleted)
  }

  def selectAllExecutionCompletes:Seq[ExecutionCompleted] = {
    dbRun(allExecutionsCompleted.result)
  }
  def insertResultSent(networkQueryId: NetworkQueryId,shrineResponse:ShrineResponse):Unit = {
    debug(s"insertResultSent $shrineResponse for $networkQueryId")

    ResultSent.fromResponse(networkQueryId,shrineResponse).foreach(insertResultSent)
  }

  def insertResultSent(resultSent: ResultSent):Unit = {
    dbRun(allResultsSent += resultSent)
  }

  def selectAllResultsSent:Seq[ResultSent] = {
    dbRun(allResultsSent.result)
  }
}

/**
 * Separate class to support schema generation without actually connecting to the database.
 *
 * @param jdbcProfile Database profile to use for the schema
 */
case class AdapterAuditSchema(jdbcProfile: JdbcProfile) extends Loggable {
  import jdbcProfile.api._

  def ddlForAllTables = {
    allQueriesReceived.schema ++
    allExecutionsStarted.schema ++
    allExecutionsCompleted.schema ++
    allResultsSent.schema
  }

  //to get the schema, use the REPL
  //println(AdapterAuditSchema.schema.ddlForAllTables.createStatements.mkString(";\n"))

  def createTables(database:Database) = {
    try {
      val future = database.run(ddlForAllTables.create)
      Await.result(future,10 seconds)
    } catch {
      //I'd prefer to check and create schema only if absent. No way to do that with Oracle.
      case x:SQLException => info("Caught exception while creating tables. Recover by assuming the tables already exist.",x)
    }
  }

  def dropTables(database:Database) = {
    val future = database.run(ddlForAllTables.drop)
    //Really wait forever for the cleanup
    Await.result(future,Duration.Inf)
  }

  class QueriesReceivedAuditTable(tag:Tag) extends Table[QueryReceived](tag,"queriesReceived") {
    def shrineNodeId = column[ShrineNodeId]("shrineNodeId")
    def userName = column[UserName]("userName")
    def networkQueryId = column[NetworkQueryId]("networkQueryId")
    def queryName = column[QueryName]("queryName")
    def queryTopicId = column[Option[QueryTopicId]]("topicId")
    def queryTopicName = column[Option[QueryTopicName]]("topicName")
    def timeQuerySent = column[Time]("timeQuerySent")
    def timeQueryReceived = column[Time]("timeReceived")

    def * = (shrineNodeId,userName,networkQueryId,queryName,queryTopicId,queryTopicName,timeQuerySent,timeQueryReceived) <> (QueryReceived.tupled,QueryReceived.unapply)
  }

  val allQueriesReceived = TableQuery[QueriesReceivedAuditTable]

  class ExecutionsStartedTable(tag:Tag) extends Table[ExecutionStarted](tag,"executionsStarted") {
    def networkQueryId = column[NetworkQueryId]("networkQueryId")
    def queryName = column[QueryName]("queryName")
    def timeExecutionStarts = column[Time]("timeExecutionStarted")

    def * = (networkQueryId,queryName,timeExecutionStarts) <> (ExecutionStarted.tupled,ExecutionStarted.unapply)
  }
  
  val allExecutionsStarted = TableQuery[ExecutionsStartedTable]

  class ExecutionsCompletedTable(tag:Tag) extends Table[ExecutionCompleted](tag,"executionsCompleted") {
    def networkQueryId = column[NetworkQueryId]("networkQueryId")
    def replyId = column[Long]("replyId")
    def queryName = column[QueryName]("queryName")
    def timeExecutionCompletes = column[Time]("timeExecutionCompleted")

    def * = (networkQueryId,replyId,queryName,timeExecutionCompletes) <> (ExecutionCompleted.tupled,ExecutionCompleted.unapply)
  }

  val allExecutionsCompleted = TableQuery[ExecutionsCompletedTable]

  class ResultsSentTable(tag:Tag) extends Table[ResultSent](tag,"resultsSent") {
    def networkQueryId = column[NetworkQueryId]("networkQueryId")
    def replyId = column[Long]("replyId")
    def queryName = column[QueryName]("queryName")
    def timeResultsSent = column[Time]("timeResultsSent")

    def * = (networkQueryId,replyId,queryName,timeResultsSent) <> (ResultSent.tupled,ResultSent.unapply)
  }
  val allResultsSent = TableQuery[ResultsSentTable]
}

object AdapterAuditSchema {

  val allConfig:Config = ConfigSource.config

  val config:Config = allConfig.getConfig("shrine.adapter.audit.database")

  val slickProfile:JdbcProfile = ConfigSource.getObject("slickProfileClassName", config)

  val schema = AdapterAuditSchema(slickProfile)
}

object AdapterAuditDb {

  val dataSource:DataSource = TestableDataSourceCreator.dataSource(AdapterAuditSchema.config)

  val db = AdapterAuditDb(AdapterAuditSchema.schema,dataSource)

  val createTablesOnStart = AdapterAuditSchema.config.getBoolean("createTablesOnStart")
  if(createTablesOnStart) AdapterAuditDb.db.createTables()

}

case class QueryReceived(
                          shrineNodeId:ShrineNodeId,
                          userName:UserName,
                          networkQueryId:NetworkQueryId,
                          queryName:QueryName,
                          queryTopicId:Option[QueryTopicId],
                          queryTopicName:Option[QueryTopicName],
                          timeQuerySent:Time,
                          timeQueryReceived:Time
                        )

object QueryReceived extends ((
  ShrineNodeId,
    UserName,
    NetworkQueryId,
    QueryName,
    Option[QueryTopicId],
    Option[QueryTopicName],
    Time,
    Time
  ) => QueryReceived) with Loggable {

  def fromBroadcastMessage(message:BroadcastMessage):Option[QueryReceived] = {
    message.request match {
      case rqr:RunQueryRequest =>

        val timestampAndShrineNodeCn:(Time,ShrineNodeId) = message.signature.fold{
          warn(s"No signature on message ${message.requestId}")
          (-1L,"No Cert For Message")}{signature =>
          val timesamp = signature.timestamp.toGregorianCalendar.getTimeInMillis
          val shrineNodeId:ShrineNodeId = KeyStoreEntry.extractCommonName(signature.value.toArray).getOrElse("Signing Cert Not Available")
          (timesamp,shrineNodeId)
        }

        Some(QueryReceived(timestampAndShrineNodeCn._2,
          message.networkAuthn.username,
          rqr.networkQueryId,
          rqr.queryDefinition.name,
          rqr.topicId,
          rqr.topicName,
          timestampAndShrineNodeCn._1,
          System.currentTimeMillis()
        ))

      case _ => None
    }
  }
}

case class ExecutionStarted(
                             networkQueryId:NetworkQueryId,
                             queryName:QueryName,
                             timeExecutionStarted:Time
                             )

object ExecutionStarted extends ((
  NetworkQueryId,
    QueryName,
    Time
  ) => ExecutionStarted){
  def fromRequest(rqr:RunQueryRequest) = {

    ExecutionStarted(rqr.networkQueryId,
      rqr.queryDefinition.name,
      System.currentTimeMillis())
  }
}

case class ExecutionCompleted(
                              networkQueryId:NetworkQueryId,
                              replyId:Long,
                              queryName:QueryName,
                              timeExecutionCompleted:Time
                             )

object ExecutionCompleted extends ((
    NetworkQueryId,
    Long,
    QueryName,
    Time
  ) => ExecutionCompleted){
  def fromRequestResponse(request: RunQueryRequest,shrineResponse:ShrineResponse) = {

    shrineResponse match {
      case rqr:RunQueryResponse => Some(ExecutionCompleted(
        request.networkQueryId,
        rqr.queryId,
        rqr.queryName,
        System.currentTimeMillis()))
      case _ => None
    }
  }
}

case class ResultSent(
                       networkQueryId:NetworkQueryId,
                       responseId:Long,
                       queryName:QueryName,
                       timeResultSent:Time
                       )

object ResultSent extends ((
  NetworkQueryId,
    Long,
    QueryName,
    Time
  ) => ResultSent){
  def fromResponse(networkQueryId:NetworkQueryId,shrineResponse:ShrineResponse) = {

    shrineResponse match {
      case rqr:RunQueryResponse => Some(ResultSent(
        networkQueryId,
        rqr.queryId,
        rqr.queryName,
        System.currentTimeMillis()))
      case _ => None
    }
  }
}

