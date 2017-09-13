package net.shrine.qep.querydb

import java.sql.SQLException
import java.util.concurrent.TimeoutException
import javax.sql.DataSource

import com.typesafe.config.Config
import net.shrine.audit.{NetworkQueryId, QueryName, Time, UserName}
import net.shrine.log.Loggable
import net.shrine.problem.{AbstractProblem, ProblemDigest, ProblemSources}
import net.shrine.protocol.{DefaultBreakdownResultOutputTypes, DeleteQueryRequest, FlagQueryRequest, I2b2ResultEnvelope, QueryMaster, QueryResult, ReadPreviousQueriesRequest, ReadPreviousQueriesResponse, RenameQueryRequest, ResultOutputType, ResultOutputTypes, RunQueryRequest, UnFlagQueryRequest}
import net.shrine.slick.{CouldNotRunDbIoActionException, TestableDataSourceCreator, TimeoutInDbIoActionException}
import net.shrine.source.ConfigSource
import net.shrine.util.XmlDateHelper
import slick.driver.JdbcProfile

import scala.collection.immutable.Iterable
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NonFatal
import scala.xml.XML

/**
  * DB code for the QEP's query instances and query results.
  *
  * @author david
  * @since 1/19/16
  */
case class QepQueryDb(schemaDef:QepQuerySchema,dataSource: DataSource,timeout:Duration) extends Loggable {
  import schemaDef._
  import jdbcProfile.api._

  val database = Database.forDataSource(dataSource)

  def createTables() = schemaDef.createTables(database)

  def dropTables() = schemaDef.dropTables(database)

  def run[R](dbio: DBIOAction[R, NoStream, _]): Future[R] = {
    database.run(dbio)
  }

  def runBlocking[R](dbio: DBIOAction[R, NoStream, _], timeout: Duration = timeout): R = {
    try {
      Await.result(this.run(dbio), timeout)
    } catch {
      case tx:TimeoutException => throw TimeoutInDbIoActionException(dataSource, timeout, tx)
      case NonFatal(x) => throw CouldNotRunDbIoActionException(dataSource, x)
    }
  }


  def runTransaction[R](dbio: DBIOAction[R, NoStream, _]): Future[R] = {
    database.run(dbio.transactionally)
  }

  def runTransactionBlocking[R](dbio: DBIOAction[R, NoStream, _], timeout: Duration = timeout): R = {
    try {
      Await.result(this.run(dbio.transactionally), timeout)
    } catch {
      case tx:TimeoutException => throw TimeoutInDbIoActionException(dataSource, timeout, tx)
      case NonFatal(x) => throw CouldNotRunDbIoActionException(dataSource, x)
    }
  }

  def insertQepQuery(runQueryRequest: RunQueryRequest):Unit = {
    debug(s"insertQepQuery $runQueryRequest")

    insertQepQuery(QepQuery(runQueryRequest))
  }

  def insertQepQuery(qepQuery: QepQuery):Unit = {
    runBlocking(allQepQueryQuery += qepQuery)
  }

  def selectAllQepQueries:Seq[QepQuery] = {
    runBlocking(mostRecentVisibleQepQueries.result)
  }

  def selectPreviousQueries(request: ReadPreviousQueriesRequest):ReadPreviousQueriesResponse = {
    val previousQueries: Seq[QepQuery] = selectPreviousQueriesByUserAndDomain(
      request.authn.username,
      request.authn.domain,
      None,
      Some(request.fetchSize))
    val flags:Map[NetworkQueryId,QepQueryFlag] = selectMostRecentQepQueryFlagsFor(previousQueries.map(_.networkId).to[Set])
    val queriesAndFlags = previousQueries.map(x => (x,flags.get(x.networkId)))

    ReadPreviousQueriesResponse(queriesAndFlags.map(x => x._1.toQueryMaster(x._2)))
  }

  def countPreviousQueriesByUserAndDomain(userName: UserName, domain: String):Int = {
    val q = mostRecentVisibleQepQueries.filter(r => r.userName === userName && r.userDomain === domain)

    runBlocking(q.size.result)
  }

  def selectQueryById(networkQueryId: NetworkQueryId): Option[QepQuery] =
    runBlocking(mostRecentVisibleQepQueries.filter(_.networkId === networkQueryId).result).lastOption

  def selectPreviousQueriesByUserAndDomain(userName: UserName, domain: String, skip:Option[Int] = None, limit:Option[Int] = None):Seq[QepQuery] = {

    debug(s"start selectPreviousQueriesByUserAndDomain $userName $domain")

    val q = mostRecentVisibleQepQueries.filter(r => r.userName === userName && r.userDomain === domain).sortBy(x => x.changeDate.desc)
    val qWithSkip = skip.fold(q)(q.drop)
    val qWithLimit = limit.fold(qWithSkip)(qWithSkip.take)

    val result = runBlocking(qWithLimit.result)

    debug(s"finished selectPreviousQueriesByUserAndDomain with $result")

    result
  }

  def renamePreviousQuery(request:RenameQueryRequest):Unit = {

    val networkQueryId = request.networkQueryId
    runTransactionBlocking(
      for {
        queryResults <- mostRecentVisibleQepQueries.filter(_.networkId === networkQueryId).result
        _ <- allQepQueryQuery ++= queryResults.map(_.copy(queryName = request.queryName,changeDate = System.currentTimeMillis()))
      } yield queryResults
    )
  }

  def markDeleted(request:DeleteQueryRequest):Unit = {

    val networkQueryId = request.networkQueryId
    runTransactionBlocking(
      for {
        queryResults <- mostRecentVisibleQepQueries.filter(_.networkId === networkQueryId).result
        _ <- allQepQueryQuery ++= queryResults.map(_.copy(deleted = true,changeDate = System.currentTimeMillis()))
      } yield queryResults
    )
  }

  def insertQepQueryFlag(flagQueryRequest: FlagQueryRequest):Unit = {
    insertQepQueryFlag(QepQueryFlag(flagQueryRequest))
  }

  def insertQepQueryFlag(unflagQueryRequest: UnFlagQueryRequest):Unit = {
    insertQepQueryFlag(QepQueryFlag(unflagQueryRequest))
  }

  def insertQepQueryFlag(qepQueryFlag: QepQueryFlag):Unit = {
    runBlocking(allQepQueryFlags += qepQueryFlag)
  }

  def selectMostRecentQepQueryFlagsFor(networkIds:Set[NetworkQueryId]):Map[NetworkQueryId,QepQueryFlag] = {
    val flags:Seq[QepQueryFlag] = runBlocking(mostRecentQueryFlags.filter(_.networkId inSet networkIds).result)

    flags.map(x => x.networkQueryId -> x).toMap
  }

  def selectMostRecentQepQueryFlagFor(networkQueryId: NetworkQueryId): Option[QepQueryFlag] =
    runBlocking(mostRecentQueryFlags.filter(_.networkId === networkQueryId).result).lastOption

  def insertQepResultRow(qepQueryRow:QueryResultRow) = {
    runBlocking(allQueryResultRows += qepQueryRow)
  }

  def insertQueryResult(networkQueryId:NetworkQueryId,result:QueryResult) = {

    val adapterNode = result.description.getOrElse(throw new IllegalStateException("description is empty, does not have an adapter node"))

    val queryResultRow = QueryResultRow(networkQueryId,result)
    val breakdowns: Iterable[QepQueryBreakdownResultsRow] = result.breakdowns.flatMap(QepQueryBreakdownResultsRow.breakdownRowsFor(networkQueryId,adapterNode,result.resultId,_))
    val problem: Seq[QepProblemDigestRow] = result.problemDigest.map(p => QepProblemDigestRow(networkQueryId,adapterNode,p.codec,p.stampText,p.summary,p.description,p.detailsXml.toString,System.currentTimeMillis())).to[Seq]

    runTransactionBlocking(
      for {
      _ <- allQueryResultRows += queryResultRow
      _ <- allBreakdownResultsRows ++= breakdowns
      _ <- allProblemDigestRows ++= problem
      } yield ()
    )
  }

  def insertQueryResultRow(queryResultRow: QueryResultRow) = {
    runBlocking(allQueryResultRows += queryResultRow)
  }

  def insertQueryResultRows(queryResultRows: Seq[QueryResultRow]) = {
    runBlocking(allQueryResultRows ++= queryResultRows)
  }

  //todo only used in tests. Is that OK?
  def selectMostRecentQepResultRowsFor(networkId:NetworkQueryId): Seq[QueryResultRow] = {
    runBlocking(mostRecentQueryResultRows.filter(_.networkQueryId === networkId).result)
  }

  def selectMostRecentFullQueryResultsFor(networkId:NetworkQueryId): Seq[FullQueryResult] = {

    val (queryResults, breakdowns,problems) = runTransactionBlocking(
      for {
        queryResults <- mostRecentQueryResultRows.filter(_.networkQueryId === networkId).result
        breakdowns: Seq[QepQueryBreakdownResultsRow] <- mostRecentBreakdownResultsRows.filter(_.networkQueryId === networkId).result
        problems <- mostRecentProblemDigestRows.filter(_.networkQueryId === networkId).result
      } yield (queryResults, breakdowns, problems)
    )

    val breakdownTypeToResults: Map[ResultOutputType, Seq[QepQueryBreakdownResultsRow]] = breakdowns.groupBy(_.resultType)

    def seqOfOneProblemRowToProblemDigest(problemSeq:Seq[QepProblemDigestRow]):ProblemDigest = {
      if(problemSeq.size == 1) problemSeq.head.toProblemDigest
      else throw new IllegalStateException(s"problemSeq size was not 1. $problemSeq")
    }

    val adapterNodesToProblemDigests: Map[String, ProblemDigest] = problems.groupBy(_.adapterNode).map(nodeToProblem => nodeToProblem._1 -> seqOfOneProblemRowToProblemDigest(nodeToProblem._2) )

    queryResults.map(r => FullQueryResult(
      r,
      breakdownTypeToResults,
      adapterNodesToProblemDigests.get(r.adapterNode)
    ))
  }

  def selectMostRecentQepResultsFor(networkId:NetworkQueryId): Seq[QueryResult] = {
    val fullQueryResults = selectMostRecentFullQueryResultsFor(networkId)

    fullQueryResults.map(_.toQueryResult)
  }

  def insertQueryBreakdown(breakdownResultsRow:QepQueryBreakdownResultsRow) = {
    runBlocking(allBreakdownResultsRows += breakdownResultsRow)
  }

  def selectAllBreakdownResultsRows: Seq[QepQueryBreakdownResultsRow] = {
    runBlocking(allBreakdownResultsRows.result)
  }

  def selectDistinctAdaptersWithResults:Seq[String] = {
   runBlocking(allQueryResultRows.map(_.adapterNode).distinct.result).sorted
  }
}

object QepQueryDb extends Loggable {

  val dataSource:DataSource = TestableDataSourceCreator.dataSource(QepQuerySchema.config)
  val timeout = QepQuerySchema.config.getInt("timeout") seconds

  val db = QepQueryDb(QepQuerySchema.schema,dataSource,timeout)

  val createTablesOnStart = QepQuerySchema.config.getBoolean("createTablesOnStart")
  if(createTablesOnStart) QepQueryDb.db.createTables()

}

/**
  * Separate class to support schema generation without actually connecting to the database.
  *
  * @param jdbcProfile Database profile to use for the schema
  */
case class QepQuerySchema(jdbcProfile: JdbcProfile,moreBreakdowns: Set[ResultOutputType]) extends Loggable {
  import jdbcProfile.api._

  def ddlForAllTables: jdbcProfile.DDL = {
    allQepQueryQuery.schema ++ allQepQueryFlags.schema ++ allQueryResultRows.schema ++ allBreakdownResultsRows.schema ++ allProblemDigestRows.schema
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
    def expression = column[Option[String]]("expression")
    def dateCreated = column[Time]("dateCreated")
    def deleted = column[Boolean]("deleted")
    def queryXml = column[String]("queryXml")
    def changeDate = column[Long]("changeDate")

    def * = (networkId,userName,userDomain,queryName,expression,dateCreated,deleted,queryXml,changeDate) <> (QepQuery.tupled,QepQuery.unapply)

  }

  val allQepQueryQuery = TableQuery[QepQueries]
  val mostRecentQepQueryQuery: Query[QepQueries, QepQuery, Seq] = for(
    queries <- allQepQueryQuery if !allQepQueryQuery.filter(_.networkId === queries.networkId).filter(_.changeDate > queries.changeDate).exists
  ) yield queries
  val mostRecentVisibleQepQueries = mostRecentQepQueryQuery.filter(_.deleted === false)

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

  val qepQueryResultTypes = DefaultBreakdownResultOutputTypes.toSet ++ ResultOutputType.values ++ moreBreakdowns
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

  class QepQueryResults(tag:Tag) extends Table[QueryResultRow](tag,"queryResults") {
    def resultId = column[Long]("resultId")
    def networkQueryId = column[NetworkQueryId]("networkQueryId")
    def instanceId = column[Long]("instanceId")
    def adapterNode = column[String]("adapterNode")
    def resultType = column[Option[ResultOutputType]]("resultType")
    def size = column[Long]("size")
    def startDate = column[Option[Long]]("startDate")
    def endDate = column[Option[Long]]("endDate")
    def status = column[QueryResult.StatusType]("status")
    def statusMessage = column[Option[String]]("statusMessage")
    def changeDate = column[Long]("changeDate")

    def * = (resultId,networkQueryId,instanceId,adapterNode,resultType,size,startDate,endDate,status,statusMessage,changeDate) <> (QueryResultRow.tupled,QueryResultRow.unapply)
  }

  val allQueryResultRows = TableQuery[QepQueryResults]

  //Most recent query result rows for each queryId from each adapter
  val mostRecentQueryResultRows: Query[QepQueryResults, QueryResultRow, Seq] = for(
    queryResultRows <- allQueryResultRows if !allQueryResultRows.filter(_.networkQueryId === queryResultRows.networkQueryId).filter(_.adapterNode === queryResultRows.adapterNode).filter(_.changeDate > queryResultRows.changeDate).exists
  ) yield queryResultRows

  class QepQueryBreakdownResults(tag:Tag) extends Table[QepQueryBreakdownResultsRow](tag,"queryBreakdownResults") {
    def networkQueryId = column[NetworkQueryId]("networkQueryId")
    def adapterNode = column[String]("adapterNode")
    def resultId = column[Long]("resultId")
    def resultType = column[ResultOutputType]("resultType")
    def dataKey = column[String]("dataKey")
    def value = column[Long]("value")
    def changeDate = column[Long]("changeDate")

    def * = (networkQueryId,adapterNode,resultId,resultType,dataKey,value,changeDate) <> (QepQueryBreakdownResultsRow.tupled,QepQueryBreakdownResultsRow.unapply)
  }

  val allBreakdownResultsRows = TableQuery[QepQueryBreakdownResults]
  //Most recent query result rows for each queryId from each adapter
  val mostRecentBreakdownResultsRows: Query[QepQueryBreakdownResults, QepQueryBreakdownResultsRow, Seq] = for(
    breakdownResultsRows <- allBreakdownResultsRows if !allBreakdownResultsRows.filter(_.networkQueryId === breakdownResultsRows.networkQueryId).filter(_.adapterNode === breakdownResultsRows.adapterNode).filter(_.resultId === breakdownResultsRows.resultId).filter(_.changeDate > breakdownResultsRows.changeDate).exists
  ) yield breakdownResultsRows

  /*
case class ProblemDigest(codec: String, stampText: String, summary: String, description: String, detailsXml: NodeSeq) extends XmlMarshaller {
    */
  class QepResultProblemDigests(tag:Tag) extends Table [QepProblemDigestRow](tag,"queryResultProblemDigests") {
    def networkQueryId = column[NetworkQueryId]("networkQueryId")
    def adapterNode = column[String]("adapterNode")
    def codec = column[String]("codec")
    def stamp = column[String]("stamp")
    def summary = column[String]("summary")
    def description = column[String]("description")
    def details = column[String]("details")
    def changeDate = column[Long]("changeDate")

    def * = (networkQueryId,adapterNode,codec,stamp,summary,description,details,changeDate) <> (QepProblemDigestRow.tupled,QepProblemDigestRow.unapply)
  }

  val allProblemDigestRows = TableQuery[QepResultProblemDigests]
  val mostRecentProblemDigestRows: Query[QepResultProblemDigests, QepProblemDigestRow, Seq] = for(
    problemDigests <- allProblemDigestRows if !allProblemDigestRows.filter(_.networkQueryId === problemDigests.networkQueryId).filter(_.adapterNode === problemDigests.adapterNode).filter(_.changeDate > problemDigests.changeDate).exists
  ) yield problemDigests

}

object QepQuerySchema {

  val allConfig:Config = ConfigSource.config
  val config:Config = allConfig.getConfig("shrine.queryEntryPoint.audit.database")

  val slickProfile:JdbcProfile = ConfigSource.getObject("slickProfileClassName", config)

  import net.shrine.config.ConfigExtensions
  val moreBreakdowns: Set[ResultOutputType] = config.getOptionConfigured("breakdownResultOutputTypes",ResultOutputTypes.fromConfig).getOrElse(Set.empty)

  val schema = QepQuerySchema(slickProfile,moreBreakdowns)
}



case class QepQuery(
                     networkId:NetworkQueryId,
                     userName: UserName,
                     userDomain: String,
                     queryName: QueryName,
                     expression: Option[String],
                     dateCreated: Time,
                     deleted: Boolean,
                     queryXml: String,
                     changeDate: Time
                   ){

  def toQueryMaster(qepQueryFlag:Option[QepQueryFlag]):QueryMaster = {

    QueryMaster(
      queryMasterId = networkId.toString,
      networkQueryId = networkId,
      name = queryName,
      userId = userName,
      groupId = userDomain,
      createDate = XmlDateHelper.toXmlGregorianCalendar(dateCreated),
      flagged = qepQueryFlag.map(_.flagged),
      flagMessage = qepQueryFlag.map(_.flagMessage)
    )
  }
}

object QepQuery extends ((NetworkQueryId,UserName,String,QueryName,Option[String],Time,Boolean,String,Time) => QepQuery) {
  def apply(runQueryRequest: RunQueryRequest):QepQuery = {
    new QepQuery(
      networkId = runQueryRequest.networkQueryId,
      userName = runQueryRequest.authn.username,
      userDomain = runQueryRequest.authn.domain,
      queryName = runQueryRequest.queryDefinition.name,
      expression = runQueryRequest.queryDefinition.expr.map(_.toString),
      dateCreated = System.currentTimeMillis(),
      deleted = false,
      queryXml = runQueryRequest.toXmlString,
      changeDate = System.currentTimeMillis()
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

//todo replace with a class per state
case class FullQueryResult(
                            resultId:Long,
                            networkQueryId:NetworkQueryId,
                            instanceId:Long,
                            adapterNode:String,
                            resultType:Option[ResultOutputType],
                            count:Long,
                            startDate:Option[Long],
                            endDate:Option[Long],
                            status:QueryResult.StatusType,
                            statusMessage:Option[String],
                            changeDate:Long,
                            breakdownTypeToResults:Map[ResultOutputType,Seq[QepQueryBreakdownResultsRow]],
                            problemDigest:Option[ProblemDigest]
                          ) {

  def toQueryResult = {
    def resultEnvelopesFrom(breakdownTypeToResults:Map[ResultOutputType,Seq[QepQueryBreakdownResultsRow]]): Map[ResultOutputType, I2b2ResultEnvelope] = {
      def resultEnvelopeFrom(resultType:ResultOutputType,breakdowns:Seq[QepQueryBreakdownResultsRow]):I2b2ResultEnvelope = {
        val data = breakdowns.map(b => b.dataKey -> b.value).toMap
        I2b2ResultEnvelope(resultType,data)
      }
      breakdownTypeToResults.map(r => r._1 -> resultEnvelopeFrom(r._1,r._2))
    }

    QueryResult(
      resultId = resultId,
      instanceId = instanceId,
      resultType = resultType,
      setSize = count,
      startDate = startDate.map(XmlDateHelper.toXmlGregorianCalendar),
      endDate = endDate.map(XmlDateHelper.toXmlGregorianCalendar),
      description = Some(adapterNode),
      statusType = status,
      statusMessage = statusMessage,
      breakdowns = resultEnvelopesFrom(breakdownTypeToResults),
      problemDigest = problemDigest
    )
  }
}

object FullQueryResult {
  def apply(row:QueryResultRow,
            breakdownTypeToResults:Map[ResultOutputType,Seq[QepQueryBreakdownResultsRow]],
            problemDigest:Option[ProblemDigest]):FullQueryResult = {
    FullQueryResult(resultId = row.resultId,
      networkQueryId = row.networkQueryId,
      instanceId = row.instanceId,
      adapterNode = row.adapterNode,
      resultType = row.resultType,
      count = row.size,
      startDate = row.startDate,
      endDate = row.endDate,
      status = row.status,
      statusMessage = row.statusMessage,
      changeDate = row.changeDate,
      breakdownTypeToResults = breakdownTypeToResults,
      problemDigest = problemDigest
    )
  }
}

case class QueryResultRow(
                           resultId:Long,
                           networkQueryId:NetworkQueryId,
                           instanceId:Long,
                           adapterNode:String,
                           resultType:Option[ResultOutputType],
                           size:Long,
                           startDate:Option[Long],
                           endDate:Option[Long],
                           status:QueryResult.StatusType,
                           statusMessage:Option[String],
                           changeDate:Long
                         ) {
}

object QueryResultRow extends ((Long,NetworkQueryId,Long,String,Option[ResultOutputType],Long,Option[Long],Option[Long],QueryResult.StatusType,Option[String],Long) => QueryResultRow)
{

  def apply(networkQueryId:NetworkQueryId,result:QueryResult):QueryResultRow = {
    new QueryResultRow(
      resultId = result.resultId,
      networkQueryId = networkQueryId,
      instanceId = result.instanceId,
      adapterNode = result.description.getOrElse(s"$result has None in its description field, instead of the name of an adapter node."),
      resultType = result.resultType,
      size = result.setSize,
      startDate = result.startDate.map(_.toGregorianCalendar.getTimeInMillis),
      endDate = result.endDate.map(_.toGregorianCalendar.getTimeInMillis),
      status = result.statusType,
      statusMessage = result.statusMessage,
      changeDate = System.currentTimeMillis()
    )
  }

}

case class QepQueryBreakdownResultsRow(
                                        networkQueryId: NetworkQueryId,
                                        adapterNode:String,
                                        resultId:Long,
                                        resultType: ResultOutputType,
                                        dataKey:String,
                                        value:Long,
                                        changeDate:Long
                                      )

object QepQueryBreakdownResultsRow extends ((NetworkQueryId,String,Long,ResultOutputType,String,Long,Long) => QepQueryBreakdownResultsRow){

  def breakdownRowsFor(networkQueryId:NetworkQueryId,
                       adapterNode:String,
                       resultId:Long,
                       breakdown:(ResultOutputType,I2b2ResultEnvelope)): Iterable[QepQueryBreakdownResultsRow] = {
    breakdown._2.data.map(b => QepQueryBreakdownResultsRow(networkQueryId,adapterNode,resultId,breakdown._1,b._1,b._2,System.currentTimeMillis()))
  }

}

case class QepProblemDigestRow(
                                networkQueryId: NetworkQueryId,
                                adapterNode: String,
                                codec: String,
                                stampText: String,
                                summary: String,
                                description: String,
                                details: String,
                                changeDate:Long
                              ){
  def toProblemDigest = {
    ProblemDigest(
      codec,
      stampText,
      summary,
      description,
      if(!details.isEmpty) XML.loadString(details)
      else <details/>,
      //TODO: FIGURE OUT HOW TO GET AN ACUTAL EPOCH INTO HERE
      0
    )
  }
}

case class QepDatabaseProblem(x:Exception) extends AbstractProblem(ProblemSources.Qep){
  override val summary = "A problem encountered while using a database."

  override val throwable = Some(x)

  override val description = x.getMessage
}