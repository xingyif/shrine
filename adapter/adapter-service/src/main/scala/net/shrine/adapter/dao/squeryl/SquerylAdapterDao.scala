package net.shrine.adapter.dao.squeryl

import javax.xml.datatype.XMLGregorianCalendar

import net.shrine.adapter.dao.AdapterDao
import net.shrine.adapter.dao.model.{ObfuscatedPair, ShrineQuery, ShrineQueryResult}
import net.shrine.adapter.dao.model.squeryl.{SquerylBreakdownResultRow, SquerylCountRow, SquerylPrivilegedUser, SquerylQueryResultRow, SquerylShrineError, SquerylShrineQuery}
import net.shrine.adapter.dao.squeryl.tables.Tables
import net.shrine.dao.DateHelpers
import net.shrine.dao.squeryl.{SquerylEntryPoint, SquerylInitializer}
import net.shrine.log.Loggable
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.protocol.{AuthenticationInfo, I2b2ResultEnvelope, QueryResult, ResultOutputType}
import net.shrine.protocol.query.QueryDefinition
import net.shrine.util.XmlDateHelper
import org.squeryl.Query
import org.squeryl.dsl.GroupWithMeasures

import scala.util.Try
import scala.xml.NodeSeq


/**
 * @author clint
 * @since May 22, 2013
 */
final class SquerylAdapterDao(initializer: SquerylInitializer, tables: Tables)(implicit breakdownTypes: Set[ResultOutputType]) extends AdapterDao with Loggable {
  initializer.init()

  override def inTransaction[T](f: => T): T = SquerylEntryPoint.inTransaction { f }

  import SquerylEntryPoint._

  override def flagQuery(networkQueryId: Long, flagMessage: Option[String]): Unit = mutateFlagField(networkQueryId, newIsFlagged = true, flagMessage)
  
  override def unFlagQuery(networkQueryId: Long): Unit = mutateFlagField(networkQueryId, newIsFlagged = false, None)
  
  private def mutateFlagField(networkQueryId: Long, newIsFlagged: Boolean, newFlagMessage: Option[String]): Unit = {
    inTransaction {
      update(tables.shrineQueries) { queryRow =>
        where(queryRow.networkId === networkQueryId).
          set(queryRow.isFlagged := newIsFlagged, queryRow.flagMessage := newFlagMessage)
      }
    }
  }
  
  override def storeResults(
    authn: AuthenticationInfo,
    masterId: String,
    networkQueryId: Long,
    queryDefinition: QueryDefinition,
    rawQueryResults: Seq[QueryResult],
    obfuscatedQueryResults: Seq[QueryResult],
    failedBreakdownTypes: Seq[ResultOutputType],
    mergedBreakdowns: Map[ResultOutputType, I2b2ResultEnvelope],
    obfuscatedBreakdowns: Map[ResultOutputType, I2b2ResultEnvelope]): Unit = {

    inTransaction {
      val insertedQueryId = insertQuery(masterId,
                                        networkQueryId,
                                        authn,
                                        queryDefinition,
                                        isFlagged = false,
                                        hasBeenRun = true,
                                        flagMessage = None)

      val insertedQueryResultIds = insertQueryResults(insertedQueryId, rawQueryResults)

      storeCountResults(rawQueryResults, obfuscatedQueryResults, insertedQueryResultIds)

      storeErrorResults(rawQueryResults, insertedQueryResultIds)

      storeBreakdownFailures(failedBreakdownTypes.toSet, insertedQueryResultIds)

      insertBreakdownResults(insertedQueryResultIds, mergedBreakdowns, obfuscatedBreakdowns)
    }
  }

  private[adapter] def storeCountResults(raw: Seq[QueryResult], obfuscated: Seq[QueryResult], insertedIds: Map[ResultOutputType, Seq[Int]]): Unit = {

    val notErrors = raw.filter(!_.isError)

    val obfuscatedNotErrors = obfuscated.filter(!_.isError)

    if(notErrors.size > 1) {
      warn(s"Got ${notErrors.size} raw (hopefully-)count results; more than 1 is unusual.")
    }
    
    if(obfuscatedNotErrors.size > 1) {
      warn(s"Got ${obfuscatedNotErrors.size} obfuscated (hopefully-)count results; more than 1 is unusual.")
    }
    
    if(notErrors.size != obfuscatedNotErrors.size) {
      warn(s"Got ${notErrors.size} raw and ${obfuscatedNotErrors.size} obfuscated (hopefully-)count results; that these numbers are different is unusual.")
    } 
    
    import ResultOutputType.PATIENT_COUNT_XML
    
    def isCount(qr: QueryResult): Boolean = qr.resultType.contains(PATIENT_COUNT_XML)
    
    inTransaction {
      //NB: Take the count/setSize from the FIRST PATIENT_COUNT_XML QueryResult, 
      //though the same count should be there for all of them, if there are more than one
      for {
        Seq(insertedCountQueryResultId) <- insertedIds.get(PATIENT_COUNT_XML)
        notError <- notErrors.find(isCount) //NB: Find a count result, just to be sure
        obfuscatedNotError <- obfuscatedNotErrors.find(isCount) //NB: Find a count result, just to be sure
      } {
        insertCountResult(insertedCountQueryResultId, notError.setSize, obfuscatedNotError.setSize)
      }
    }
  }

  private[adapter] def storeErrorResults(results: Seq[QueryResult], insertedIds: Map[ResultOutputType, Seq[Int]]): Unit = {

    val errors = results.filter(_.isError)

    val insertedErrorResultIds = insertedIds.getOrElse(ResultOutputType.ERROR,Nil)

    val insertedIdsToErrors = insertedErrorResultIds zip errors
    
    inTransaction {
      for {
        (insertedErrorResultId, errorQueryResult) <- insertedIdsToErrors
      } {
        val pd = errorQueryResult.problemDigest.get //it's an error so it will have a problem digest

        insertErrorResult(
          insertedErrorResultId,
          errorQueryResult.statusMessage.getOrElse("Unknown failure"),
          pd.codec,
          pd.stampText,
          pd.summary,
          pd.description,
          pd.detailsXml
        )
      }
    }
  }

  private[adapter] def storeBreakdownFailures(failedBreakdownTypes: Set[ResultOutputType], insertedIds: Map[ResultOutputType, Seq[Int]]): Unit = {
    val insertedIdsForFailedBreakdownTypes = insertedIds.filterKeys(failedBreakdownTypes.contains)
    
    inTransaction {
      for {
        (failedBreakdownType, Seq(resultId)) <- insertedIdsForFailedBreakdownTypes
      } {
        //todo propagate backwards to the breakdown failure to create the corect problem
        object BreakdownFailure extends AbstractProblem(ProblemSources.Adapter) {
          override val summary: String = "Couldn't retrieve result breakdown"
          override val description:String = s"Couldn't retrieve result breakdown of type '$failedBreakdownType'"
        }

        val pd = BreakdownFailure.toDigest

        insertErrorResult(
          resultId,
          s"Couldn't retrieve breakdown of type '$failedBreakdownType'",
          pd.codec,
          pd.stampText,
          pd.summary,
          pd.description,
          pd.detailsXml
        )
      }
    }
  }

  override def findRecentQueries(howMany: Int): Seq[ShrineQuery] = {
    inTransaction {
      Queries.queriesForAllUsers.take(howMany).map(_.toShrineQuery).toSeq
    }
  }

  def findAllCounts():Seq[SquerylCountRow] = {
    inTransaction{
      Queries.allCountResults.toSeq
    }
  }

  override def renameQuery(networkQueryId: Long, newName: String) {
    inTransaction {
      update(tables.shrineQueries) { queryRow =>
        where(queryRow.networkId === networkQueryId).
          set(queryRow.name := newName)
      }
    }
  }

  override def deleteQuery(networkQueryId: Long): Unit = {
    inTransaction {
      tables.shrineQueries.deleteWhere(_.networkId === networkQueryId)
    }
  }

  override def deleteQueryResultsFor(networkQueryId: Long): Unit = {
    inTransaction {
      val resultIdsForNetworkQueryId = join(tables.shrineQueries, tables.queryResults) { (queryRow, resultRow) =>
        where(queryRow.networkId === networkQueryId).
          select(resultRow.id).
          on(queryRow.id === resultRow.queryId)
      }.toSet

      tables.queryResults.deleteWhere(_.id in resultIdsForNetworkQueryId)
    }
  }

  override def isUserLockedOut(authn: AuthenticationInfo, defaultThreshold: Int): Boolean = Try {
    inTransaction {
      val privilegedUserOption = Queries.privilegedUsers(authn.domain, authn.username).singleOption

      val threshold:Int = privilegedUserOption.flatMap(_.threshold).getOrElse(defaultThreshold.intValue)

      val thirtyDaysInThePast: XMLGregorianCalendar = DateHelpers.daysFromNow(-30)

      val overrideDate: XMLGregorianCalendar = privilegedUserOption.map(_.toPrivilegedUser).flatMap(_.overrideDate).getOrElse(thirtyDaysInThePast)

      //sorted instead of just finding max
      val counts: Seq[Long] = Queries.repeatedResults(authn.domain, authn.username, overrideDate).toSeq.sorted

      //and then grabbing the last, highest value in the sorted sequence
      val repeatedResultCount: Long = counts.lastOption.getOrElse(0L)

      val result = repeatedResultCount > threshold

      debug(s"User ${authn.domain}:${authn.username} locked out? $result")

      result
    }
  }.getOrElse(false)

  override def insertQuery(localMasterId: String,
                           networkId: Long,
                           authn: AuthenticationInfo,
                           queryDefinition: QueryDefinition,
                           isFlagged: Boolean,
                           hasBeenRun: Boolean,
                           flagMessage: Option[String]): Int = {
    inTransaction {
      val inserted = tables.shrineQueries.insert(new SquerylShrineQuery(
                                                  0,
                                                  localMasterId,
                                                  networkId,
                                                  authn.username,
                                                  authn.domain,
                                                  XmlDateHelper.now,
                                                  isFlagged,
                                                  flagMessage,
                                                  hasBeenRun,
                                                  queryDefinition))

      inserted.id
    }
  }

  /**
   * Insert rows into QueryResults, one for each QueryResult in the passed RunQueryResponse
   * Inserted rows are 'children' of the passed ShrineQuery (ie, they are the results of the query)
   */
  override def insertQueryResults(parentQueryId: Int, results: Seq[QueryResult]): Map[ResultOutputType, Seq[Int]] = {
    def execTime(result: QueryResult): Option[Long] = {
      //TODO: How are locales handled here?  Do we care?
      def toMillis(xmlGc: XMLGregorianCalendar) = xmlGc.toGregorianCalendar.getTimeInMillis

      for {
        start <- result.startDate
        end <- result.endDate
      } yield toMillis(end) - toMillis(start)
    }

    val typeToIdTuples = inTransaction {
      for {
        result <- results
        resultType = result.resultType.getOrElse(ResultOutputType.ERROR)
        //TODO: under what circumstances can QueryResults NOT have start and end dates set?
        elapsed = execTime(result)
      } yield {
        val lastInsertedQueryResultRow = tables.queryResults.insert(new SquerylQueryResultRow(0, result.resultId, parentQueryId, resultType, result.statusType, elapsed, XmlDateHelper.now))

        (resultType, lastInsertedQueryResultRow.id)
      }
    }

    typeToIdTuples.groupBy { case (resultType, _) => resultType }.mapValues(_.map { case (_, count) => count })
  }

  override def insertCountResult(resultId: Int, originalCount: Long, obfuscatedCount: Long) {
    //NB: Squeryl steers us toward inserting with dummy ids :(
    inTransaction {
      tables.countResults.insert(new SquerylCountRow(0, resultId, originalCount, obfuscatedCount, XmlDateHelper.now))
    }
  }

  override def insertBreakdownResults(parentResultIds: Map[ResultOutputType, Seq[Int]], originalBreakdowns: Map[ResultOutputType, I2b2ResultEnvelope], obfuscatedBreakdowns: Map[ResultOutputType, I2b2ResultEnvelope]) {
    def merge(original: I2b2ResultEnvelope, obfuscated: I2b2ResultEnvelope): Map[String, ObfuscatedPair] = {
      Map.empty ++ (for {
        (key, originalValue) <- original.data
        obfuscatedValue <- obfuscated.data.get(key)
      } yield (key, ObfuscatedPair(originalValue, obfuscatedValue)))
    }

    inTransaction {
      for {
        (resultType, Seq(resultId)) <- parentResultIds 
        if resultType.isBreakdown
        originalBreakdown <- originalBreakdowns.get(resultType)
        obfuscatedBreakdown <- obfuscatedBreakdowns.get(resultType)
        (key, ObfuscatedPair(original, obfuscated)) <- merge(originalBreakdown, obfuscatedBreakdown)
      } {
        tables.breakdownResults.insert(SquerylBreakdownResultRow(0, resultId, key, original, obfuscated))
      }
    }
  }

  override def insertErrorResult(parentResultId: Int, errorMessage: String, codec:String, stampText:String, summary:String, digestDescription:String,detailsXml:NodeSeq) {
    //NB: Squeryl steers us toward inserting with dummy ids :(
    inTransaction {
      tables.errorResults.insert(SquerylShrineError(0, parentResultId, errorMessage, codec, stampText, summary, digestDescription, detailsXml.toString()))
    }
  }

  override def findQueryByNetworkId(networkQueryId: Long): Option[ShrineQuery] = {
    inTransaction {
      Queries.queriesByNetworkId(networkQueryId).headOption.map(_.toShrineQuery)
    }
  }

  override def findQueriesByUserAndDomain(domain: String, username: String, howMany: Int): Seq[ShrineQuery] = {
    inTransaction {
      Queries.queriesForUser(username, domain).take(howMany).toSeq.map(_.toShrineQuery)
    }
  }

  override def findQueriesByDomain(domain: String): Seq[ShrineQuery] = {
    inTransaction {
      Queries.queriesForDomain(domain).toList.map(_.toShrineQuery)
    }
  }

  override def findResultsFor(networkQueryId: Long): Option[ShrineQueryResult] = {
    inTransaction {
      val breakdownRowsByType = Queries.breakdownResults(networkQueryId).toSeq.groupBy { case (outputType, _) => outputType.toQueryResultRow.resultType }.mapValues(_.map { case (_, row) => row.toBreakdownResultRow })

      val queryRowOption = Queries.queriesByNetworkId(networkQueryId).headOption.map(_.toShrineQuery)
      val countRowOption = Queries.countResults(networkQueryId).headOption.map(_.toCountRow)
      val queryResultRows = Queries.resultsForQuery(networkQueryId).toSeq.map(_.toQueryResultRow)
      val errorResultRows = Queries.errorResults(networkQueryId).toSeq.map(_.toShrineError)
      
      for {
        queryRow <- queryRowOption
        countRow <- countRowOption
        shrineQueryResult <- ShrineQueryResult.fromRows(queryRow, queryResultRows, countRow, breakdownRowsByType, errorResultRows)
      } yield {
        shrineQueryResult
      }
    }
  }

  /**
   * @author clint
   * @since Nov 19, 2012
   */
  object Queries {

    def privilegedUsers(domain: String, username: String): Query[SquerylPrivilegedUser] = {
      from(tables.privilegedUsers) { user =>
        where(user.username === username and user.domain === domain).select(user)
      }
    }

    def repeatedResults(domain: String, username: String, overrideDate: XMLGregorianCalendar): Query[Long] = {

      val counts: Query[GroupWithMeasures[Long, Long]] = join(tables.shrineQueries, tables.queryResults, tables.countResults) { (queryRow, resultRow, countRow) =>
        where(queryRow.username === username and queryRow.domain === domain and (countRow.originalValue <> 0L) and queryRow.dateCreated > DateHelpers.toTimestamp(overrideDate)).
          groupBy(countRow.originalValue).
          compute(count(countRow.originalValue)).
          on(queryRow.id === resultRow.queryId, resultRow.id === countRow.resultId)
      }

      //Filter for result counts > 0
      from(counts) { cnt =>
        where(cnt.measures gt 0).select(cnt.measures)
      }
    }

    val queriesForAllUsers: Query[SquerylShrineQuery] = {
      from(tables.shrineQueries) { queryRow =>
        select(queryRow).orderBy(queryRow.dateCreated.desc)
      }
    }

    //TODO: Find a way to parameterize on limit, to avoid building the query every time
    //TODO: limit
    def queriesForUser(username: String, domain: String): Query[SquerylShrineQuery] = {
      from(tables.shrineQueries) { queryRow =>
        where(queryRow.domain === domain and queryRow.username === username).
          select(queryRow).
          orderBy(queryRow.dateCreated.desc)
      }
    }

    def queriesForDomain(domain: String): Query[SquerylShrineQuery] = {
      from(tables.shrineQueries) { queryRow =>
        where(queryRow.domain === domain).
          select(queryRow).
          orderBy(queryRow.dateCreated.desc)
      }
    }

    val allCountResults: Query[SquerylCountRow] = {
      from(tables.countResults) { queryRow =>
        select(queryRow)
      }
    }

    def queriesByNetworkId(networkQueryId: Long): Query[SquerylShrineQuery] = {
      from(tables.shrineQueries) { queryRow =>
        where(queryRow.networkId === networkQueryId).select(queryRow)
      }
    }

    //TODO: Find out how to compose queries, to re-use queriesByNetworkId
    def queryNamesByNetworkId(networkQueryId: Long): Query[String] = {
      from(tables.shrineQueries) { queryRow =>
        where(queryRow.networkId === networkQueryId).select(queryRow.name)
      }
    }

    def resultsForQuery(networkQueryId: Long): Query[SquerylQueryResultRow] = {
      val resultsForNetworkQueryId = join(tables.shrineQueries, tables.queryResults) { (queryRow, resultRow) =>
        where(queryRow.networkId === networkQueryId).
          select(resultRow).
          on(queryRow.id === resultRow.queryId)
      }

      from(resultsForNetworkQueryId)(select(_))
    }

    def countResults(networkQueryId: Long): Query[SquerylCountRow] = {
      join(tables.shrineQueries, tables.queryResults, tables.countResults) { (queryRow, resultRow, countRow) =>
        where(queryRow.networkId === networkQueryId).
          select(countRow).
          on(queryRow.id === resultRow.queryId, resultRow.id === countRow.resultId)
      }
    }

    def errorResults(networkQueryId: Long): Query[SquerylShrineError] = {
      join(tables.shrineQueries, tables.queryResults, tables.errorResults) { (queryRow, resultRow, errorRow) =>
        where(queryRow.networkId === networkQueryId).
          select(errorRow).
          on(queryRow.id === resultRow.queryId, resultRow.id === errorRow.resultId)
      }
    }

    //NB: using groupBy here is too much of a pain; do it 'manually' later
    def breakdownResults(networkQueryId: Long): Query[(SquerylQueryResultRow, SquerylBreakdownResultRow)] = {
      join(tables.shrineQueries, tables.queryResults, tables.breakdownResults) { (queryRow, resultRow, breakdownRow) =>
        where(queryRow.networkId === networkQueryId).
          select((resultRow, breakdownRow)).
          on(queryRow.id === resultRow.queryId, resultRow.id === breakdownRow.resultId)
      }
    }
  }
}