package net.shrine.qep.queries

import java.sql.SQLException
import java.util.GregorianCalendar
import javax.sql.DataSource
import javax.xml.datatype.DatatypeFactory

import com.typesafe.config.Config
import net.shrine.audit.{NetworkQueryId, QueryName, Time, UserName}
import net.shrine.log.Loggable
import net.shrine.protocol.{QueryResult, ResultOutputType, DefaultBreakdownResultOutputTypes, UnFlagQueryRequest, FlagQueryRequest, QueryMaster, ReadPreviousQueriesRequest, ReadPreviousQueriesResponse, RunQueryRequest}
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

  //todo order
  def selectPreviousQueries(request: ReadPreviousQueriesRequest):ReadPreviousQueriesResponse = {
    val previousQueries: Seq[QepQuery] = selectPreviousQueriesByUserAndDomain(request.authn.username,request.authn.domain)
    val flags:Map[NetworkQueryId,QepQueryFlag] = selectMostRecentQepQueryFlagsFor(previousQueries.map(_.networkId).to[Set])
    val queriesAndFlags = previousQueries.map(x => (x,flags.get(x.networkId)))

    ReadPreviousQueriesResponse(queriesAndFlags.map(x => x._1.toQueryMaster(x._2)))
  }

  //todo order
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
    dbRun(allQepQueryFlags += qepQueryFlag)
  }

  def selectMostRecentQepQueryFlagsFor(networkIds:Set[NetworkQueryId]):Map[NetworkQueryId,QepQueryFlag] = {
    val flags:Seq[QepQueryFlag] = dbRun(mostRecentQueryFlags.filter(_.networkId inSet networkIds).result)

    flags.map(x => x.networkQueryId -> x).toMap
  }

  def insertQepResultRow(qepQueryRow:QueryResultRow) = {
    dbRun(allQueryResultRows += qepQueryRow)
  }

  def insertQueryResult(result:QueryResult) = {
//todo     insertQepResultRow(QueryResultRow(result))
  }

  def selectMostRecentQepResultRowsFor(networkId:NetworkQueryId): Seq[QueryResultRow] = {
    dbRun(mostRecentQueryResultRows.filter(_.networkQueryId === networkId).result)
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
    allQepQueryQuery.schema ++ allQepQueryFlags.schema ++ allQueryResultRows.schema
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

  class QepQueries(tag:Tag) extends Table[QepQuery](tag,"previousQueries") {
    def networkId = column[NetworkQueryId]("networkId")
    def userName = column[UserName]("userName")
    def userDomain = column[String]("domain")
    def queryName = column[QueryName]("queryName")
    def expression = column[String]("expression")
    def dateCreated = column[Time]("dateCreated")
    def queryXml = column[String]("queryXml")

    def * = (networkId,userName,userDomain,queryName,expression,dateCreated,queryXml) <> (QepQuery.tupled,QepQuery.unapply)

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

  /**
    * The adapter's QUERY_RESULTS table looks like this:
    *
    * mysql> describe QUERY_RESULT;
+--------------+------------------------------------------------------------------------------------------------------------------------------------------------------------+------+-----+-------------------+----------------+
| Field        | Type                                                                                                                                                       | Null | Key | Default           | Extra          |
+--------------+------------------------------------------------------------------------------------------------------------------------------------------------------------+------+-----+-------------------+----------------+
| id           | int(11)                                                                                                                                                    | NO   | PRI | NULL              | auto_increment |
| local_id     | varchar(255)                                                                                                                                               | NO   |     | NULL              |                |
| query_id     | int(11)                                                                                                                                                    | NO   | MUL | NULL              |                |
| type         | enum('PATIENTSET','PATIENT_COUNT_XML','PATIENT_AGE_COUNT_XML','PATIENT_RACE_COUNT_XML','PATIENT_VITALSTATUS_COUNT_XML','PATIENT_GENDER_COUNT_XML','ERROR') | NO   |     | NULL              |                |
| status       | enum('FINISHED','ERROR','PROCESSING','QUEUED')                                                                                                             | NO   |     | NULL              |                |
| time_elapsed | int(11)                                                                                                                                                    | YES  |     | NULL              |                |
| last_updated | timestamp                                                                                                                                                  | NO   |     | CURRENT_TIMESTAMP |                |
+--------------+------------------------------------------------------------------------------------------------------------------------------------------------------------+------+-----+-------------------+----------------+

    */

  val qepQueryResultTypes = DefaultBreakdownResultOutputTypes.toSet ++ ResultOutputType.values
  val stringsToQueryResultTypes: Map[String, ResultOutputType] = qepQueryResultTypes.map(x => (x.name,x)).toMap
  val queryResultTypesToString: Map[ResultOutputType, String] = stringsToQueryResultTypes.map(_.swap)

  implicit val qepQueryResultTypesColumnType = MappedColumnType.base[ResultOutputType,String] ({
    (resultType: ResultOutputType) => queryResultTypesToString(resultType)
  },{
    (string: String) => stringsToQueryResultTypes(string)
  })

  implicit val queryStatusColumnType = MappedColumnType.base[QueryResult.StatusType,String] ({
    statusType => statusType.name
  },{
    name => QueryResult.StatusType.valueOf(name).getOrElse(throw new IllegalStateException(s"$name is not one of ${QueryResult.StatusType.values.map(_.name).mkString(", ")}"))
  })

  //todo what of these actually get used?
  class QepQueryResults(tag:Tag) extends Table[QueryResultRow](tag,"queryResults") {
    def resultId = column[Long]("resultId")
    def localId = column[String]("localId")
    def networkQueryId = column[NetworkQueryId]("networkQueryId")
    def adapterNode = column[String]("adapterNode")
    def resultType = column[ResultOutputType]("resultType")
    def setSize = column[Long]("size")
    def startDate = column[Option[Long]]("startDate")
    def endDate = column[Option[Long]]("endDate")
    def description = column[Option[String]]("description")
    def status = column[QueryResult.StatusType]("status")
    def statusMessage = column[Option[String]]("statusMessage")
    def changeDate = column[Long]("changeDate")

    def * = (resultId,localId,networkQueryId,adapterNode,resultType,setSize,startDate,endDate,description,status,statusMessage,changeDate) <> (QueryResultRow.tupled,QueryResultRow.unapply)
  }

  val allQueryResultRows = TableQuery[QepQueryResults]

  //Most recent query result rows for each queryId from each adapter
  val mostRecentQueryResultRows: Query[QepQueryResults, QueryResultRow, Seq] = for(
    queryResultRows <- allQueryResultRows if !allQueryResultRows.filter(_.networkQueryId === queryResultRows.networkQueryId).filter(_.adapterNode === queryResultRows.adapterNode).filter(_.changeDate > queryResultRows.changeDate).exists
  ) yield queryResultRows


  /*
    with some other aux tables to hold specifics:

    mysql> describe COUNT_RESULT;
+------------------+-----------+------+-----+-------------------+----------------+
| Field            | Type      | Null | Key | Default           | Extra          |
+------------------+-----------+------+-----+-------------------+----------------+
| id               | int(11)   | NO   | PRI | NULL              | auto_increment |
| result_id        | int(11)   | NO   | MUL | NULL              |                |
| original_count   | int(11)   | NO   |     | NULL              |                |
| obfuscated_count | int(11)   | NO   |     | NULL              |                |
| date_created     | timestamp | NO   |     | CURRENT_TIMESTAMP |                |
+------------------+-----------+------+-----+-------------------+----------------+

    mysql> describe BREAKDOWN_RESULT;
+------------------+--------------+------+-----+---------+----------------+
| Field            | Type         | Null | Key | Default | Extra          |
+------------------+--------------+------+-----+---------+----------------+
| id               | int(11)      | NO   | PRI | NULL    | auto_increment |
| result_id        | int(11)      | NO   | MUL | NULL    |                |
| data_key         | varchar(255) | NO   |     | NULL    |                |
| original_value   | int(11)      | NO   |     | NULL    |                |
| obfuscated_value | int(11)      | NO   |     | NULL    |                |
+------------------+--------------+------+-----+---------+----------------+

    mysql> describe ERROR_RESULT;
+---------------------+--------------+------+-----+--------------------------+----------------+
| Field               | Type         | Null | Key | Default                  | Extra          |
+---------------------+--------------+------+-----+--------------------------+----------------+
| id                  | int(11)      | NO   | PRI | NULL                     | auto_increment |
| result_id           | int(11)      | NO   | MUL | NULL                     |                |
| message             | varchar(255) | NO   |     | NULL                     |                |
| CODEC               | varchar(256) | NO   |     | Pre-1.20 Error           |                |
| SUMMARY             | text         | NO   |     | NULL                     |                |
| DESCRIPTION         | text         | NO   |     | NULL                     |                |
| PROBLEM_DESCRIPTION | text         | NO   |     | NULL                     |                |
| DETAILS             | text         | NO   |     | NULL                     |                |
| STAMP               | varchar(256) | NO   |     | Unknown time and machine |                |
+---------------------+--------------+------+-----+--------------------------+----------------+

    */



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
      held = None, //todo if a query is held at the adapter, how will we know? do we care? Question out to Bill and leadership
      flagged = qepQueryFlag.map(_.flagged),
      flagMessage = qepQueryFlag.map(_.flagMessage)
    )
  }
}

object QepQuery extends ((NetworkQueryId,UserName,String,QueryName,String,Time,String) => QepQuery) {
  def apply(runQueryRequest: RunQueryRequest):QepQuery = {
    new QepQuery(
      networkId = runQueryRequest.networkQueryId,
      userName = runQueryRequest.authn.username,
      userDomain = runQueryRequest.authn.domain,
      queryName = runQueryRequest.queryDefinition.name,
      expression = runQueryRequest.queryDefinition.expr.getOrElse("No Expression").toString,
      dateCreated = System.currentTimeMillis(),
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
      flagged = false,
      flagMessage = "",
      changeDate = System.currentTimeMillis()
    )
  }

}

/*
  resultId: Long,
  instanceId: Long,
  resultType: Option[ResultOutputType],
  setSize: Long,
  startDate: Option[XMLGregorianCalendar],
  endDate: Option[XMLGregorianCalendar],
  description: Option[String],
  statusType: StatusType,
  statusMessage: Option[String],

  //todo problemDigest in a separate table
  problemDigest: Option[ProblemDigest] = None,

  //todo breakdowns in a separate table
  breakdowns: Map[ResultOutputType,I2b2ResultEnvelope] = Map.empty
 */

case class QueryResultRow(
                          resultId:Long,
                          localId:String,
                          networkQueryId:NetworkQueryId, //the query's instanceId //todo verify
                          adapterNode:String,
                          resultType:ResultOutputType,
                          setSize:Long,
                          startDate:Option[Long],
                          endDate:Option[Long],
                          description:Option[String],
                          status:QueryResult.StatusType,
                          statusMessage:Option[String],
                          changeDate:Long
                         ) {

}

/*
object QueryResultRow {

  def apply(result:QueryResult):QueryResultRow = {
    QueryResultRow(

    )
  }

}
*/