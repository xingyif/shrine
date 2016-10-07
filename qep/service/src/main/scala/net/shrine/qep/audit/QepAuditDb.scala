package net.shrine.qep.audit

import java.sql.SQLException
import javax.sql.DataSource

import com.typesafe.config.Config
import net.shrine.audit.{NetworkQueryId, QueryName, QueryTopicId, QueryTopicName, ShrineNodeId, Time, UserName}
import net.shrine.log.Loggable
import net.shrine.protocol.RunQueryRequest
import net.shrine.qep.QepConfigSource
import net.shrine.slick.{NeedsWarmUp, TestableDataSourceCreator}
import slick.driver.JdbcProfile

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future, blocking}
import scala.language.postfixOps

/**
 * DB code for the QEP audit metrics.
 *
 * @author david
 * @since 8/18/15
 */
case class QepAuditDb(schemaDef:QepAuditSchema,dataSource: DataSource) extends Loggable {
  import schemaDef._
  import jdbcProfile.api._

  val database = Database.forDataSource(dataSource)

  def warmUp = dbRun(allQepQueryQuery.size.result)

  def createTables() = schemaDef.createTables(database)

  def dropTables() = schemaDef.dropTables(database)

  def dbRun[R](action: DBIOAction[R, NoStream, Nothing]):R = {
    val future: Future[R] = database.run(action)
    blocking {
      Await.result(future, 10 seconds)
    }
  }

  def insertQepQuery(runQueryRequest:RunQueryRequest,commonName:String):Unit = {
    debug(s"insertQepQuery $runQueryRequest")

    insertQepQuery(QepQueryAuditData.fromRunQueryRequest(runQueryRequest,commonName))
  }

  def insertQepQuery(qepQueryAuditData: QepQueryAuditData):Unit = {
    dbRun(allQepQueryQuery += qepQueryAuditData)
  }

  def selectAllQepQueries:Seq[QepQueryAuditData] = {
    dbRun(allQepQueryQuery.result)
  }

}

object QepAuditDb extends Loggable with NeedsWarmUp {

  val dataSource:DataSource = TestableDataSourceCreator.dataSource(QepAuditSchema.config)

  val db = QepAuditDb(QepAuditSchema.schema,dataSource)

  val createTablesOnStart = QepAuditSchema.config.getBoolean("createTablesOnStart")
  if(createTablesOnStart) QepAuditDb.db.createTables()

  def warmUp() = db.warmUp
  // Todo, call this higher up
  warmUp()

}

/**
 * Separate class to support schema generation without actually connecting to the database.
 *
 * @param jdbcProfile Database profile to use for the schema
 */
case class QepAuditSchema(jdbcProfile: JdbcProfile) extends Loggable {
  import jdbcProfile.api._

  def ddlForAllTables = {
    allQepQueryQuery.schema
  }

  //to get the schema, use the REPL
  //println(QepAuditSchema.schema.ddlForAllTables.createStatements.mkString(";\n"))

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

  class QueriesSent(tag:Tag) extends Table[QepQueryAuditData](tag,"queriesSent") {
    def shrineNodeId = column[ShrineNodeId]("shrineNodeId")
    def userName = column[UserName]("userName")
    def networkQueryId = column[NetworkQueryId]("networkQueryId")
    def queryName = column[QueryName]("queryName")
    def queryTopicId = column[Option[QueryTopicId]]("queryTopicId")
    def queryTopicName = column[Option[QueryTopicName]]("queryTopicName")
    def timeQuerySent = column[Time]("timeQuerySent")

    def * = (shrineNodeId,userName,networkQueryId,queryName,queryTopicId,queryTopicName,timeQuerySent) <> (QepQueryAuditData.tupled,QepQueryAuditData.unapply)

  }
  val allQepQueryQuery = TableQuery[QueriesSent]
}

object QepAuditSchema {

  val allConfig:Config = QepConfigSource.config
  val config:Config = allConfig.getConfig("shrine.queryEntryPoint.audit.database")

  val slickProfile:JdbcProfile = QepConfigSource.getObject("slickProfileClassName", config)

  val schema = QepAuditSchema(slickProfile)
}


/**
 * Container for QEP audit data for ACT metrics
 *
 * @author david
 * @since 8/17/15
 */
case class QepQueryAuditData(
                              shrineNodeId:ShrineNodeId,
                              userName:UserName,
                              networkQueryId:NetworkQueryId,
                              queryName:QueryName,
                              queryTopicId:Option[QueryTopicId],
                              queryTopicName:Option[QueryTopicName],
                              timeQuerySent:Time
                            ) {}

object QepQueryAuditData extends ((
                                    ShrineNodeId,
                                    UserName,
                                    NetworkQueryId,
                                    QueryName,
                                    Option[QueryTopicId],
                                    Option[QueryTopicName],
                                    Time
                                  ) => QepQueryAuditData) {

  def apply(
             shrineNodeId:String,
             userName:String,
             networkQueryId:Long,
             queryName:String,
             queryTopicId:Option[String],
             queryTopicName: Option[QueryTopicName]
             ):QepQueryAuditData = QepQueryAuditData(
                                                      shrineNodeId,
                                                      userName,
                                                      networkQueryId,
                                                      queryName,
                                                      queryTopicId,
                                                      queryTopicName,
                                                      System.currentTimeMillis()
                                                    )

  def fromRunQueryRequest(request:RunQueryRequest,commonName:String):QepQueryAuditData = {
    QepQueryAuditData(
      commonName,
      request.authn.username,
      request.networkQueryId,
      request.queryDefinition.name,
      request.topicId,
      request.topicName
    )
  }

}