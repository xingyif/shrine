package net.shrine.adapter.audit

import java.io.PrintWriter
import java.sql.{DriverManager, Connection, SQLException}
import java.util.logging.Logger
import javax.naming.InitialContext
import javax.sql.DataSource

import com.typesafe.config.Config
import net.shrine.adapter.service.AdapterConfigSource
import net.shrine.crypto.KeyStoreCertCollection
import net.shrine.log.Loggable
import net.shrine.audit.{QueryTopicId, Time, QueryName, NetworkQueryId, UserName, ShrineNodeId}
import net.shrine.protocol.{BroadcastMessage, RunQueryRequest, RunQueryResponse, ShrineResponse}

import slick.driver.JdbcProfile
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.{Duration,DurationInt}
import scala.language.postfixOps

import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.blocking

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

  def insertResultSent(resultSent: ResultSent):Unit = {
    dbRun(allResultsSent += resultSent)
  }

  def selectAllResultsSent:Seq[ResultSent] = {
    dbRun(allResultsSent.result)
  }

  def insertExecutionStarted(executionStart:ExecutionStarted):Unit = {
    dbRun(allExecutionStarts += executionStart)
  }

  def selectAllExecutionStarts:Seq[ExecutionStarted] = {
    dbRun(allExecutionStarts.result)
  }

  def insertExecutionCompleted(executionCompleted:ExecutionCompleted):Unit = {
    dbRun(allExecutionCompletes += executionCompleted)
  }

  def selectAllExecutionCompletes:Seq[ExecutionCompleted] = {
    dbRun(allExecutionCompletes.result)
  }

  def insertQueryReceived(queryReceived:QueryReceived):Unit = {
    dbRun(allQueriesReceived += queryReceived)
  }

  def selectAllQueriesReceived:Seq[QueryReceived] = {
    dbRun(allQueriesReceived.result)
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
    allResultsSent.schema ++
      allExecutionStarts.schema ++
      allExecutionCompletes.schema ++
      allQueriesReceived.schema
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

  class ResultsSentTable(tag:Tag) extends Table[ResultSent](tag,"resultsSent") {
    def networkQueryId = column[NetworkQueryId]("networkQueryId")
    def queryName = column[QueryName]("queryName")
    def timeQuerySent = column[Time]("timeQuerySent")

    def * = (networkQueryId,queryName,timeQuerySent) <> (ResultSent.tupled,ResultSent.unapply)
  }
  val allResultsSent = TableQuery[ResultsSentTable]
  
  class ExecutionStartsTable(tag:Tag) extends Table[ExecutionStarted](tag,"executionStarts") {
    def networkQueryId = column[NetworkQueryId]("networkQueryId")
    def queryName = column[QueryName]("queryName")
    def timeQuerySent = column[Time]("timeQuerySent")

    def * = (networkQueryId,queryName,timeQuerySent) <> (ExecutionStarted.tupled,ExecutionStarted.unapply)
  }
  
  val allExecutionStarts = TableQuery[ExecutionStartsTable]

  class ExecutionCompletesTable(tag:Tag) extends Table[ExecutionCompleted](tag,"executionCompletes") {
    def networkQueryId = column[NetworkQueryId]("networkQueryId")
    def queryName = column[QueryName]("queryName")
    def timeQuerySent = column[Time]("timeQuerySent")

    def * = (networkQueryId,queryName,timeQuerySent) <> (ExecutionCompleted.tupled,ExecutionCompleted.unapply)
  }

  val allExecutionCompletes = TableQuery[ExecutionCompletesTable]

  class QueriesReceivedAuditTable(tag:Tag) extends Table[QueryReceived](tag,"queryReceived") {
    def shrineNodeId = column[ShrineNodeId]("shrineNodeId")
    def userName = column[UserName]("userName")
    def networkQueryId = column[NetworkQueryId]("networkQueryId")
    def queryName = column[QueryName]("queryName")
    def timeQuerySent = column[Time]("timeSent")
    def queryTopicId = column[Option[QueryTopicId]]("topicId")
    def timeQueryReceived = column[Time]("timeReceived")

    def * = (shrineNodeId,userName,networkQueryId,queryName,timeQuerySent,queryTopicId,timeQueryReceived) <> (QueryReceived.tupled,QueryReceived.unapply)
  }

  val allQueriesReceived = TableQuery[QueriesReceivedAuditTable]


}

object AdapterAuditSchema {

  val allConfig:Config = AdapterConfigSource.config
  val config:Config = allConfig.getConfig("shrine.adapter2.audit.database")

  val slickProfileClassName = config.getString("slickProfileClassName")
  val slickProfile:JdbcProfile = AdapterConfigSource.objectForName(slickProfileClassName)

  val schema = AdapterAuditSchema(slickProfile)
}

object AdapterAuditDb {

  val dataSource:DataSource = {

    val dataSourceFrom = AdapterAuditSchema.config.getString("dataSourceFrom")
    if(dataSourceFrom == "JNDI") {
      val jndiDataSourceName = AdapterAuditSchema.config.getString("jndiDataSourceName")
      val initialContext:InitialContext = new InitialContext()

      initialContext.lookup(jndiDataSourceName).asInstanceOf[DataSource]

    }
    else if (dataSourceFrom == "testDataSource") {

      val testDataSourceConfig = AdapterAuditSchema.config.getConfig("testDataSource")
      val driverClassName = testDataSourceConfig.getString("driverClassName")
      val url = testDataSourceConfig.getString("url")

      //Creating an instance of the driver register it. (!) From a previous epoch, but it works.
      Class.forName(driverClassName).newInstance()

      object TestDataSource extends DataSource {
        override def getConnection: Connection = {
          DriverManager.getConnection(url)
        }

        override def getConnection(username: String, password: String): Connection = {
          DriverManager.getConnection(url, username, password)
        }

        //unused methods
        override def unwrap[T](iface: Class[T]): T = ???
        override def isWrapperFor(iface: Class[_]): Boolean = ???
        override def setLogWriter(out: PrintWriter): Unit = ???
        override def getLoginTimeout: Int = ???
        override def setLoginTimeout(seconds: Int): Unit = ???
        override def getParentLogger: Logger = ???
        override def getLogWriter: PrintWriter = ???
      }

      TestDataSource
    }
    else throw new IllegalArgumentException(s"shrine.steward.database.dataSourceFrom must be either JNDI or testDataSource, not $dataSourceFrom")
  }

  val db = AdapterAuditDb(AdapterAuditSchema.schema,dataSource)

  val createTablesOnStart = AdapterAuditSchema.config.getBoolean("createTablesOnStart")
  if(createTablesOnStart) AdapterAuditDb.db.createTables()

}

case class ResultSent(
                       networkQueryId:NetworkQueryId,
                       queryName:QueryName,
                       timeQueryResponse:Time
                       )

object ResultSent extends ((
  NetworkQueryId,
    QueryName,
    Time
  ) => ResultSent){
  def fromResponse(shrineResponse:ShrineResponse) = {

    shrineResponse match {
      case rqr:RunQueryResponse => Some(ResultSent(rqr.queryId,
        rqr.queryName,
        System.currentTimeMillis()))
      case _ => None
    }
  }
}

case class ExecutionStarted(
                             networkQueryId:NetworkQueryId,
                             queryName:QueryName,
                             timeQueryResponse:Time
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
                              queryName:QueryName,
                              timeQueryResponse:Time
                              )

object ExecutionCompleted extends ((
  NetworkQueryId,
    QueryName,
    Time
  ) => ExecutionCompleted){
  def fromResponse(shrineResponse:ShrineResponse) = {

    shrineResponse match {
      case rqr:RunQueryResponse => Some(ExecutionCompleted(rqr.queryId,
        rqr.queryName,
        System.currentTimeMillis()))
      case _ => None
    }
  }
}

case class QueryReceived(
                          shrineNodeId:ShrineNodeId,
                          userName:UserName,
                          networkQueryId:NetworkQueryId,
                          queryName:QueryName,
                          timeQuerySent:Time,
                          queryTopicId:Option[QueryTopicId],
                          timeQueryReceived:Time
                          )

object QueryReceived extends ((
  ShrineNodeId,
    UserName,
    NetworkQueryId,
    QueryName,
    Time,
    Option[QueryTopicId],
    Time
  ) => QueryReceived) with Loggable {

  def fromBroadcastMessage(message:BroadcastMessage):Option[QueryReceived] = {
    message.request match {
      case rqr:RunQueryRequest =>

        val timestampAndShrineNodeCn:(Time,ShrineNodeId) = message.signature.fold{
          warn(s"No signature on message ${message.requestId}")
          (-1L,"No Cert For Message")}{signature =>
          val timesamp = signature.timestamp.toGregorianCalendar.getTimeInMillis
          val shrineNodeId:ShrineNodeId = signature.signingCert.fold("Signing Cert Not Available")(x => KeyStoreCertCollection.extractCommonName(x.toCertificate).getOrElse("Common name not in cert"))
          (timesamp,shrineNodeId)
        }

        Some(QueryReceived(timestampAndShrineNodeCn._2,
          message.networkAuthn.username,
          rqr.networkQueryId,
          rqr.queryDefinition.name,
          timestampAndShrineNodeCn._1,
          rqr.topicId,
          System.currentTimeMillis()
        ))

      case _ => None
    }
  }
}