package net.shrine.qep.queries

import java.sql.SQLException
import java.util.GregorianCalendar
import javax.sql.DataSource
import javax.xml.datatype.DatatypeFactory

import com.typesafe.config.Config
import net.shrine.audit.{NetworkQueryId, QueryName, Time, UserName}
import net.shrine.log.Loggable
import net.shrine.protocol.{UnFlagQueryRequest, FlagQueryRequest, QueryMaster, ReadPreviousQueriesRequest, ReadPreviousQueriesResponse, RunQueryRequest}
import net.shrine.qep.QepConfigSource
import net.shrine.slick.TestableDataSourceCreator
import slick.driver.JdbcProfile

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future, blocking}
import scala.language.postfixOps

/**
  * DB code for the QEP's query instances and query results.
  *
  * @author david
  * @since 1/19/16
  */
case class QepQueryDb(schemaDef:QepQuerySchema,dataSource: DataSource) extends Loggable {
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

  def insertQepQuery(runQueryRequest: RunQueryRequest):Unit = {
    debug(s"insertQepQuery $runQueryRequest")

    insertQepQuery(QepQuery(runQueryRequest))
  }

  def insertQepQuery(qepQuery: QepQuery):Unit = {
    dbRun(allQepQueryQuery += qepQuery)
  }

  def selectAllQepQueries:Seq[QepQuery] = {
    dbRun(allQepQueryQuery.result)
  }

  def selectPreviousQueries(request: ReadPreviousQueriesRequest):ReadPreviousQueriesResponse = {
    val previousQueries: Seq[QepQuery] = selectPreviousQueriesByUserAndDomain(request.authn.username,request.authn.domain)
    val flags:Map[NetworkQueryId,QepQueryFlag] = selectMostRecentQepQueryFlagsFor(previousQueries.map(_.networkId).to[Set])
    val queriesAndFlags = previousQueries.map(x => (x,flags.get(x.networkId)))

    ReadPreviousQueriesResponse(queriesAndFlags.map(x => x._1.toQueryMaster(x._2)))
  }

  def selectPreviousQueriesByUserAndDomain(userName: UserName,domain: String):Seq[QepQuery] = {
    dbRun(allQepQueryQuery.filter(_.userName === userName).filter(_.userDomain === domain).result)
  }

  def insertQepQueryFlag(flagQueryRequest: FlagQueryRequest):Unit = {
    insertQepQueryFlag(QepQueryFlag(flagQueryRequest))
  }

  def insertQepQueryFlag(unflagQueryRequest: UnFlagQueryRequest):Unit = {
    insertQepQueryFlag(QepQueryFlag(unflagQueryRequest))
  }

  def insertQepQueryFlag(qepQueryFlag: QepQueryFlag):Unit = {
    debug(s"insertQepQueryFlag $qepQueryFlag")
    dbRun(allQepQueryFlags += qepQueryFlag)
  }

  def selectMostRecentQepQueryFlagsFor(networkIds:Set[NetworkQueryId]):Map[NetworkQueryId,QepQueryFlag] = {
    val flags:Seq[QepQueryFlag] = dbRun(mostRecentQueryFlags.filter(_.networkId inSet networkIds).result)

    flags.map(x => x.networkQueryId -> x).toMap
  }
}

object QepQueryDb extends Loggable {

  val dataSource:DataSource = TestableDataSourceCreator.dataSource(QepQuerySchema.config)

  val db = QepQueryDb(QepQuerySchema.schema,dataSource)

  val createTablesOnStart = QepQuerySchema.config.getBoolean("createTablesOnStart")
  if(createTablesOnStart) QepQueryDb.db.createTables()

}

/**
  * Separate class to support schema generation without actually connecting to the database.
  *
  * @param jdbcProfile Database profile to use for the schema
  */
case class QepQuerySchema(jdbcProfile: JdbcProfile) extends Loggable {
  import jdbcProfile.api._

  def ddlForAllTables: jdbcProfile.DDL = {
    allQepQueryQuery.schema ++ allQepQueryFlags.schema
  }

  //to get the schema, use the REPL
  //println(QepQuerySchema.schema.ddlForAllTables.createStatements.mkString(";\n"))

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

  /**
  mysql> describe SHRINE_QUERY;
+------------------+--------------+------+-----+-------------------+----------------+
| Field            | Type         | Null | Key | Default           | Extra          |
+------------------+--------------+------+-----+-------------------+----------------+
| id               | int(11)      | NO   | PRI | NULL              | auto_increment |
| local_id         | varchar(255) | NO   | MUL | NULL              |                |
| network_id       | bigint(20)   | NO   | MUL | NULL              |                |
| username         | varchar(255) | NO   | MUL | NULL              |                |
| domain           | varchar(255) | NO   |     | NULL              |                |
| query_name       | varchar(255) | NO   |     | NULL              |                |
| query_expression | text         | YES  |     | NULL              |                |
| date_created     | timestamp    | NO   |     | CURRENT_TIMESTAMP |                |
| has_been_run     | tinyint(1)   | NO   |     | 0                 |                |
| flagged          | tinyint(1)   | NO   |     | 0                 |                |
| flag_message     | varchar(255) | YES  |     | NULL              |                |
| query_xml        | text         | YES  |     | NULL              |                |
+------------------+--------------+------+-----+-------------------+----------------+
    */

  class QepQueries(tag:Tag) extends Table[QepQuery](tag,"previousQueries") {
    def networkId = column[NetworkQueryId]("networkId")
    def userName = column[UserName]("userName")
    def userDomain = column[String]("domain")
    def queryName = column[QueryName]("queryName")
    def expression = column[String]("expression")
    def dateCreated = column[Time]("dateCreated")
    def hasBeenRun = column[Boolean]("hasBeenRun")
    def queryXml = column[String]("queryXml")

    def * = (networkId,userName,userDomain,queryName,expression,dateCreated,hasBeenRun,queryXml) <> (QepQuery.tupled,QepQuery.unapply)

  }

  val allQepQueryQuery = TableQuery[QepQueries]

  class QepQueryFlags(tag:Tag) extends Table[QepQueryFlag](tag,"queryFlags") {
    def networkId = column[NetworkQueryId]("networkId")
    def flagged = column[Boolean]("flagged")
    def flagMessage = column[String]("flagMessage")
    def changeDate = column[Long]("changeDate")

    def * = (networkId,flagged,flagMessage,changeDate) <> (QepQueryFlag.tupled,QepQueryFlag.unapply)
  }

  val allQepQueryFlags = TableQuery[QepQueryFlags]
  val mostRecentQueryFlags: Query[QepQueryFlags, QepQueryFlag, Seq] = for(
    queryFlags <- allQepQueryFlags if !allQepQueryFlags.filter(_.networkId === queryFlags.networkId).filter(_.changeDate > queryFlags.changeDate).exists
  ) yield queryFlags
}

object QepQuerySchema {

  val allConfig:Config = QepConfigSource.config
  val config:Config = allConfig.getConfig("shrine.queryEntryPoint.audit.database")

  val slickProfileClassName = config.getString("slickProfileClassName")
  val slickProfile:JdbcProfile = QepConfigSource.objectForName(slickProfileClassName)

  val schema = QepQuerySchema(slickProfile)
}

case class QepQuery(
                     networkId:NetworkQueryId,
                     userName: UserName,
                     userDomain: String,
                     queryName: QueryName,
                     expression: String,
                     dateCreated: Time,
                     hasBeenRun: Boolean,
                     queryXml:String
                   ){

  def toQueryMaster(qepQueryFlag:Option[QepQueryFlag]):QueryMaster = {

    val gregorianCalendar = new GregorianCalendar()
    gregorianCalendar.setTimeInMillis(dateCreated)
    val xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar)
    QueryMaster(
      queryMasterId = networkId.toString,
      networkQueryId = networkId,
      name = queryName,
      userId = userName,
      groupId = userDomain,
      createDate = xmlGregorianCalendar,
      held = None, //todo if a query is held at the adapter, how will we know? do we care?
      flagged = qepQueryFlag.map(_.flagged),
      flagMessage = qepQueryFlag.map(_.flagMessage)
    )
  }
}

object QepQuery extends ((NetworkQueryId,UserName,String,QueryName,String,Time,Boolean,String) => QepQuery) {
  def apply(runQueryRequest: RunQueryRequest):QepQuery = {
    new QepQuery(
      networkId = runQueryRequest.networkQueryId,
      userName = runQueryRequest.authn.username,
      userDomain = runQueryRequest.authn.domain,
      queryName = runQueryRequest.queryDefinition.name,
      expression = runQueryRequest.queryDefinition.expr.getOrElse("No Expression").toString,
      dateCreated = System.currentTimeMillis(),
      hasBeenRun = false,  //todo ??
      queryXml = runQueryRequest.toXmlString
    )
  }
}

case class QepQueryFlag(
                       networkQueryId: NetworkQueryId,
                       flagged:Boolean,
                       flagMessage:String,
                       changeDate:Long
                       )

object QepQueryFlag extends ((NetworkQueryId,Boolean,String,Long) => QepQueryFlag) {
  def apply(flagQueryRequest: FlagQueryRequest):QepQueryFlag = {
    QepQueryFlag(
      networkQueryId = flagQueryRequest.networkQueryId,
      flagged = true,
      flagMessage = flagQueryRequest.message.getOrElse(""),
      changeDate = System.currentTimeMillis()
    )
  }

  def apply(unflagQueryRequest: UnFlagQueryRequest):QepQueryFlag = {
    QepQueryFlag(
      networkQueryId = unflagQueryRequest.networkQueryId,
      flagged = true,
      flagMessage = "",
      changeDate = System.currentTimeMillis()
    )
  }

}

