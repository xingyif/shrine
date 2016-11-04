package net.shrine.steward.db

import java.sql.SQLException
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

import com.typesafe.config.Config
import net.shrine.authorization.steward.{Date, ExternalQueryId, InboundShrineQuery, InboundTopicRequest, OutboundShrineQuery, OutboundTopic, OutboundUser, QueriesPerUser, QueryContents, QueryHistory, ResearcherToAudit, ResearchersTopics, StewardQueryId, StewardsTopics, TopicId, TopicIdAndName, TopicState, TopicStateName, TopicsPerState, UserName, researcherRole, stewardRole}
import net.shrine.i2b2.protocol.pm.User
import net.shrine.log.Loggable
import net.shrine.slick.{NeedsWarmUp, TestableDataSourceCreator}
import net.shrine.steward.{CreateTopicsMode, StewardConfigSource}
import slick.dbio.Effect.Read
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future, blocking}
import scala.language.postfixOps
import scala.util.Try
/**
 * Database access code for the data steward service.
 *
 * I'm not letting Slick handle foreign key resolution for now. I want to keep that logic separate to handle dirty data with some grace.
 *
 * @author dwalend
 * @since 1.19
 */
case class StewardDatabase(schemaDef:StewardSchema,dataSource: DataSource) extends Loggable {
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

  def warmUp = {
    dbRun(allUserQuery.size.result)
  }

  def selectUsers:Seq[UserRecord] = {
    dbRun(allUserQuery.result)
  }

  // todo use whenever a shrine query is logged
  def upsertUser(user:User):Unit = {

    val userRecord = UserRecord(user)
    dbRun(allUserQuery.insertOrUpdate(userRecord))
  }

  def createRequestForTopicAccess(user:User,topicRequest:InboundTopicRequest):TopicRecord = {
    val createInState = StewardConfigSource.createTopicsInState
    val now = System.currentTimeMillis()
    val topicRecord = TopicRecord(Some(nextTopicId.getAndIncrement),topicRequest.name,topicRequest.description,user.username,now,createInState.topicState)
    val userTopicRecord = UserTopicRecord(user.username,topicRecord.id.get,TopicState.approved,user.username,now)

    dbRun(for{
      _ <- allTopicQuery += topicRecord
      _ <- allUserTopicQuery += userTopicRecord
    } yield topicRecord)
  }

  def updateRequestForTopicAccess(user:User,topicId:TopicId,topicRequest:InboundTopicRequest):Try[OutboundTopic] = Try {

    dbRun(mostRecentTopicQuery.filter(_.id === topicId).result.headOption.flatMap{ option =>
        val oldTopicRecord = option.getOrElse(throw TopicDoesNotExist(topicId = topicId))
        if(user.username != oldTopicRecord.createdBy) throw DetectedAttemptByWrongUserToChangeTopic(topicId,user.username,oldTopicRecord.createdBy)
        if(oldTopicRecord.state == TopicState.approved) throw ApprovedTopicCanNotBeChanged(topicId)

        val updatedTopic = oldTopicRecord.copy(name = topicRequest.name,
                                                description = topicRequest.description,
                                                changedBy = user.username,
                                                changeDate = System.currentTimeMillis())
        (allTopicQuery += updatedTopic).flatMap{_ =>
          outboundUsersForNamesAction(Set(updatedTopic.createdBy,updatedTopic.changedBy)).map(updatedTopic.toOutboundTopic)
        }
      }
    )
  }

  def selectTopicsForResearcher(parameters:QueryParameters):ResearchersTopics = {
    require(parameters.researcherIdOption.isDefined,"A researcher's parameters must supply a user id")

    val (count,topics,userNamesToOutboundUsers) = dbRun(
      for{
        count <- topicCountQuery(parameters).length.result
        topics <- topicSelectQuery(parameters).result
        userNamesToOutboundUsers <- outboundUsersForNamesAction((topics.map(_.createdBy) ++ topics.map(_.changedBy)).to[Set])
      } yield (count, topics,userNamesToOutboundUsers))

    ResearchersTopics(parameters.researcherIdOption.get,
      count,
      parameters.skipOption.getOrElse(0),
      topics.map(_.toOutboundTopic(userNamesToOutboundUsers)))
  }

  //treat as private (currently used in test)
  def selectTopics(queryParameters: QueryParameters):Seq[TopicRecord] = {
    dbRun(topicSelectQuery(queryParameters).result)
  }

  def selectTopicsForSteward(queryParameters: QueryParameters):StewardsTopics = {

    val (count,topics,userNamesToOutboundUsers) = dbRun{
      for{
        count <- topicCountQuery(queryParameters).length.result
        topics <- topicSelectQuery(queryParameters).result
        userNamesToOutboundUsers <- outboundUsersForNamesAction((topics.map(_.createdBy) ++ topics.map(_.changedBy)).to[Set])
      } yield (count,topics,userNamesToOutboundUsers)
    }

    StewardsTopics(count,
                    queryParameters.skipOption.getOrElse(0),
                    topics.map(_.toOutboundTopic(userNamesToOutboundUsers)))
  }

  private def topicSelectQuery(queryParameters: QueryParameters):Query[TopicTable, TopicTable#TableElementType, Seq] = {
    val countFilter = topicCountQuery(queryParameters)

    //todo is there some way to do something with a map from column names to columns that I don't have to update? I couldn't find one.
    //    val orderByQuery = queryParameters.sortByOption.fold(countFilter)(
    //      columnName => limitFilter.sortBy(x => queryParameters.sortOrder.orderForColumn(countFilter.columnForName(columnName))))
    val orderByQuery = queryParameters.sortByOption.fold(countFilter)(
      columnName => countFilter.sortBy(x => queryParameters.sortOrder.orderForColumn(columnName match {
        case "id" => x.id
        case "name" => x.name
        case "description" => x.description
        case "createdBy" => x.createdBy
        case "createDate" => x.createDate
        case "state" => x.state
        case "changedBy" => x.changedBy
        case "changeDate" => x.changeDate
      })))

    val skipFilter = queryParameters.skipOption.fold(orderByQuery)(skip => orderByQuery.drop(skip))
    val limitFilter = queryParameters.limitOption.fold(skipFilter)(limit => skipFilter.take(limit))

    limitFilter
  }

  private def topicCountQuery(queryParameters: QueryParameters):Query[TopicTable, TopicTable#TableElementType, Seq] = {
    val allTopics:Query[TopicTable, TopicTable#TableElementType, Seq] = mostRecentTopicQuery
    val researcherFilter = queryParameters.researcherIdOption.fold(allTopics)(userId => allTopics.filter(_.createdBy === userId))
    val stateFilter = queryParameters.stateOption.fold(researcherFilter)(state => researcherFilter.filter(_.state === state.name))
    val minDateFilter = queryParameters.minDate.fold(stateFilter)(minDate => stateFilter.filter(_.changeDate >= minDate))
    val maxDateFilter = queryParameters.maxDate.fold(minDateFilter)(maxDate => minDateFilter.filter(_.changeDate <= maxDate))

    maxDateFilter
  }

  def changeTopicState(topicId:TopicId,state:TopicState,userId:UserName):Option[TopicRecord] = {

    val noTopicRecord:Option[TopicRecord] = None
    val noOpDBIO:DBIOAction[Option[TopicRecord], NoStream, Effect.Write] = DBIO.successful(noTopicRecord)

    dbRun(mostRecentTopicQuery.filter(_.id === topicId).result.headOption.flatMap(
      _.fold(noOpDBIO){ originalTopic =>
        val updatedTopic = originalTopic.copy(state = state, changedBy = userId, changeDate = System.currentTimeMillis())
        (allTopicQuery += updatedTopic).map(_ => Option(updatedTopic))
      }
    ))
  }

  def selectTopicCountsPerState(queryParameters: QueryParameters):TopicsPerState = {
    dbRun(for{
      totalTopics <- topicCountQuery(queryParameters).length.result
      topicsPerStateName <- topicCountsPerState(queryParameters).result
    } yield TopicsPerState(totalTopics,topicsPerStateName))
  }

  private def topicCountsPerState(queryParameters: QueryParameters): Query[(Rep[TopicStateName], Rep[Int]), (TopicStateName, Int), Seq] = {
    val groupedByState = topicCountQuery(queryParameters).groupBy(topicRecord => topicRecord.state)
    groupedByState.map{case (state,result) => (state,result.length)}
  }

  def logAndCheckQuery(userId:UserName,topicId:Option[TopicId],shrineQuery:InboundShrineQuery):(TopicState,Option[TopicIdAndName]) = {

    //todo upsertUser(user) when the info is available from the PM
    val noOpDBIOForState: DBIOAction[TopicState, NoStream, Effect.Read] = DBIO.successful {
      if (StewardConfigSource.createTopicsInState == CreateTopicsMode.TopicsIgnoredJustLog) TopicState.approved
      else TopicState.createTopicsModeRequiresTopic
    }

    val noOpDBIOForTopicName: DBIOAction[Option[String], NoStream, Read] = DBIO.successful{None}

    val (state,topicName) = dbRun(for{

      state <- topicId.fold(noOpDBIOForState)( someTopicId =>
          mostRecentTopicQuery.filter(_.id === someTopicId).filter(_.createdBy === userId).map(_.state).result.headOption.map(
            _.fold(TopicState.unknownForUser)(state => TopicState.namesToStates(state)))
        )
      topicName <- topicId.fold(noOpDBIOForTopicName)( someTopicId =>
       mostRecentTopicQuery.filter(_.id === someTopicId).filter(_.createdBy === userId).map(_.name).result.headOption
      )
      _ <- allQueryTable += ShrineQueryRecord(userId,topicId,shrineQuery,state)
    } yield (state,topicName))

    val topicIdAndName:Option[TopicIdAndName] = (topicId,topicName) match {
      case (Some(id),Some(name)) => Option(TopicIdAndName(id.toString,name))
      case (None,None) => None
      case (Some(id),None) =>
        if(state == TopicState.unknownForUser) None
        else throw new IllegalStateException(s"How did you get here for $userId with $id and $state for $shrineQuery")
      case (None,Some(name)) =>
        if(state == TopicState.unknownForUser) None
        else throw new IllegalStateException(s"How did you get here for $userId with no topic id but a topic name of $name and $state for $shrineQuery")
    }
    (state,topicIdAndName)
  }

  def selectQueryHistory(queryParameters: QueryParameters,topicParameter:Option[TopicId]):QueryHistory = {

    val (count,shrineQueries,topics,userNamesToOutboundUsers) = dbRun(for {
      count <- shrineQueryCountQuery(queryParameters,topicParameter).length.result
      shrineQueries <- shrineQuerySelectQuery(queryParameters, topicParameter).result
      topics <- mostRecentTopicQuery.filter(_.id.inSet(shrineQueries.map(_.topicId).to[Set].flatten)).result
      userNamesToOutboundUsers <- outboundUsersForNamesAction(shrineQueries.map(_.userId).to[Set] ++ (topics.map(_.createdBy) ++ topics.map(_.changedBy)).to[Set])

    } yield (count,shrineQueries,topics,userNamesToOutboundUsers))

    val topicIdsToTopics: Map[Option[TopicId], TopicRecord] = topics.map(x => (x.id, x)).toMap

    def toOutboundShrineQuery(queryRecord: ShrineQueryRecord): OutboundShrineQuery = {
      val topic = topicIdsToTopics.get(queryRecord.topicId)
      val outboundTopic: Option[OutboundTopic] = topic.map(_.toOutboundTopic(userNamesToOutboundUsers))

      val outboundUserOption = userNamesToOutboundUsers.get(queryRecord.userId)
      //todo if a user is unknown and the system is in a mode that requires everyone to log into the data steward notify the data steward
      val outboundUser: OutboundUser = outboundUserOption.getOrElse(OutboundUser.createUnknownUser(queryRecord.userId))

      queryRecord.createOutboundShrineQuery(outboundTopic, outboundUser)
    }

    val result = QueryHistory(count,queryParameters.skipOption.getOrElse(0),shrineQueries.map(toOutboundShrineQuery))
    result
  }

  private def outboundUsersForNamesAction(userNames:Set[UserName]):DBIOAction[Map[UserName, OutboundUser], NoStream, Read] = {
    allUserQuery.filter(_.userName.inSet(userNames)).result.map(_.map(x => (x.userName,x.asOutboundUser)).toMap)
  }

  private def shrineQuerySelectQuery(queryParameters: QueryParameters,topicParameter:Option[TopicId]):Query[QueryTable, QueryTable#TableElementType, Seq] = {
    val countQuery = shrineQueryCountQuery(queryParameters,topicParameter)

    //todo is there some way to do something with a map from column names to columns that I don't have to update? I couldn't find one.
    //    val orderByQuery = queryParameters.sortByOption.fold(limitFilter)(
    //      columnName => limitFilter.sortBy(x => queryParameters.sortOrder.orderForColumn(allQueryTable.columnForName(columnName))))
    val orderByQuery = queryParameters.sortByOption.fold(countQuery) {
      case "topicName" =>
        val joined = countQuery.join(mostRecentTopicQuery).on(_.topicId === _.id)
        joined.sortBy(x => queryParameters.sortOrder.orderForColumn(x._2.name)).map(x => x._1)
      case columnName => countQuery.sortBy(x => queryParameters.sortOrder.orderForColumn(columnName match {
        case "stewardId" => x.stewardId
        case "externalId" => x.externalId
        case "researcherId" => x.researcherId
        case "name" => x.name
        case "topic" => x.topicId
        case "queryContents" => x.queryContents
        case "stewardResponse" => x.stewardResponse
        case "date" => x.date
      }))
    }

    val skipFilter = queryParameters.skipOption.fold(orderByQuery)(skip => orderByQuery.drop(skip))
    val limitFilter = queryParameters.limitOption.fold(skipFilter)(limit => skipFilter.take(limit))

    limitFilter
  }

  private def shrineQueryCountQuery(queryParameters: QueryParameters,topicParameter:Option[TopicId]):Query[QueryTable, QueryTable#TableElementType, Seq] = {

    val allShrineQueries:Query[QueryTable, QueryTable#TableElementType, Seq] = allQueryTable

    val topicFilter:Query[QueryTable, QueryTable#TableElementType, Seq] = topicParameter.fold(allShrineQueries)(topicId => allShrineQueries.filter(_.topicId === topicId))

    val researcherFilter:Query[QueryTable, QueryTable#TableElementType, Seq] = queryParameters.researcherIdOption.fold(topicFilter)(researcherId => topicFilter.filter(_.researcherId === researcherId))
    //todo this is probably a binary Approved/Not approved
    val stateFilter:Query[QueryTable, QueryTable#TableElementType, Seq] = queryParameters.stateOption.fold(researcherFilter)(stewardResponse => researcherFilter.filter(_.stewardResponse === stewardResponse.name))

    val minDateFilter = queryParameters.minDate.fold(stateFilter)(minDate => stateFilter.filter(_.date >= minDate))
    val maxDateFilter = queryParameters.maxDate.fold(minDateFilter)(maxDate => minDateFilter.filter(_.date <= maxDate))

    maxDateFilter
  }

  def selectShrineQueryCountsPerUser(queryParameters: QueryParameters):QueriesPerUser = {

    val (totalQueries,queriesPerUser,userNamesToOutboundUsers) = dbRun(for {
      totalQueries <- shrineQueryCountQuery(queryParameters,None).length.result
      queriesPerUser <- shrineQueryCountsPerResearcher(queryParameters).result
      userNamesToOutboundUsers <- outboundUsersForNamesAction(queriesPerUser.map(x => x._1).to[Set])
    } yield (totalQueries,queriesPerUser,userNamesToOutboundUsers))

    val queriesPerOutboundUser:Seq[(OutboundUser,Int)] = queriesPerUser.map(x => (userNamesToOutboundUsers(x._1),x._2))

    QueriesPerUser(totalQueries,queriesPerOutboundUser)
  }

  private def shrineQueryCountsPerResearcher(queryParameters: QueryParameters): Query[(Rep[UserName],Rep[Int]),(UserName,Int),Seq] = {
    val filteredShrineQueries:Query[QueryTable, QueryTable#TableElementType, Seq] = shrineQueryCountQuery(queryParameters,None)
    val groupedByResearcher = filteredShrineQueries.groupBy(shrineQuery => shrineQuery.researcherId)
    groupedByResearcher.map{case (researcher,result) => (researcher,result.length)}
  }

  lazy val nextTopicId:AtomicInteger = new AtomicInteger({
    dbRun(allTopicQuery.map(_.id).max.result).getOrElse(0) + 1
  })

  def selectAllAuditRequests: Seq[UserAuditRecord] = {
    dbRun(allUserAudits.result)
  }

  def selectMostRecentAuditRequests: Seq[UserAuditRecord] = {
    dbRun(mostRecentUserAudits.result)
  }

  def selectResearchersToAudit(maxQueryCountBetweenAudits:Int,minTimeBetweenAudits:Duration,now:Date):Seq[ResearcherToAudit] = {

    //todo one round with the db instead of O(researchers)

    //for each researcher
    //horizon = if the researcher has had an audit
    //                date of last audit
    //             else if no audit yet
    //                date of first query
    val researchersToHorizons: Map[UserName, Date] = dbRun(for{
      dateOfFirstQuery: Seq[(UserName, Date)] <- leastRecentUserQuery.map(record => record.researcherId -> record.date).result
      mostRecentAudit: Seq[(UserName, Date)] <- mostRecentUserAudits.map(record => record.researcher -> record.changeDate).result
    } yield {
      dateOfFirstQuery.toMap ++ mostRecentAudit.toMap
    })

    val researchersToHorizonsAndCounts = researchersToHorizons.map{ researcherDate =>

      val queryParameters = QueryParameters(researcherIdOption = Some(researcherDate._1),
                                            minDate = Some(researcherDate._2))

      val count:Int = dbRun(shrineQueryCountQuery(queryParameters,None).length.result)
      (researcherDate._1,(researcherDate._2,count))
    }

    //audit if oldest query within the horizon is >= minTimeBetweenAudits in the past and the researcher has run at least one query since.
    val oldestAllowed = System.currentTimeMillis() - minTimeBetweenAudits.toMillis
    val timeBasedAudit = researchersToHorizonsAndCounts.filter(x => x._2._2 > 0 && x._2._1 <= oldestAllowed)

    //audit if the researcher has run >= maxQueryCountBetweenAudits queries since horizon?
    val queryBasedAudit = researchersToHorizonsAndCounts.filter(x => x._2._2 >= maxQueryCountBetweenAudits)

    val toAudit = timeBasedAudit ++ queryBasedAudit

    val namesToOutboundUsers: Map[UserName, OutboundUser] = dbRun(outboundUsersForNamesAction(toAudit.keySet))

    toAudit.map(x => ResearcherToAudit(namesToOutboundUsers(x._1),x._2._2,x._2._1,now)).to[Seq]
  }

  def logAuditRequests(auditRequests:Seq[ResearcherToAudit],now:Date) {
    dbRun{
      allUserAudits ++= auditRequests.map(x => UserAuditRecord(researcher = x.researcher.userName,
                                                                queryCount = x.count,
                                                                changeDate = now
                                                              ))
    }
  }

}

/**
 * Separate class to support schema generation without actually connecting to the database.
 *
 * @param jdbcProfile Database profile to use for the schema
 */
case class StewardSchema(jdbcProfile: JdbcProfile) extends Loggable {
  import jdbcProfile.api._

  def ddlForAllTables = {
    allUserQuery.schema ++ allTopicQuery.schema ++ allQueryTable.schema ++ allUserTopicQuery.schema ++ allUserAudits.schema
  }

  //to get the schema, use the REPL
  //println(StewardSchema.schema.ddlForAllTables.createStatements.mkString(";\n"))

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

  class UserTable(tag:Tag) extends Table[UserRecord](tag,"users") {
    def userName = column[UserName]("userName",O.PrimaryKey)
    def fullName = column[String]("fullName")
    def isSteward = column[Boolean]("isSteward")

    def * = (userName,fullName,isSteward) <> (UserRecord.tupled,UserRecord.unapply)
  }

  class TopicTable(tag:Tag) extends Table[TopicRecord](tag,"topics") {
    def id = column[TopicId]("id")
    def name = column[String]("name")
    def description = column[String]("description")
    def createdBy = column[UserName]("createdBy")
    def createDate = column[Date]("createDate")
    def state = column[TopicStateName]("state")
    def changedBy = column[UserName]("changedBy")
    def changeDate = column[Date]("changeDate")

    def idIndex = index("idIndex",id,unique = false)
    def topicNameIndex = index("topicNameIndex",name,unique = false)
    def createdByIndex = index("createdByIndex",createdBy,unique = false)
    def createDateIndex = index("createDateIndex",createDate,unique = false)
    def stateIndex = index("stateIndex",state,unique = false)
    def changedByIndex = index("changedByIndex",changedBy,unique = false)
    def changeDateIndex = index("changeDateIndex",changeDate,unique = false)

    def * = (id.?, name, description, createdBy, createDate, state, changedBy, changeDate) <> (fromRow, toRow) //(TopicRecord.tupled,TopicRecord.unapply)

    def fromRow = (fromParams _).tupled

    def fromParams(id:Option[TopicId] = None,
                   name:String,
                   description:String,
                   createdBy:UserName,
                   createDate:Date,
                   stateName:String,
                   changedBy:UserName,
                   changeDate:Date): TopicRecord = {
      TopicRecord(id, name, description, createdBy, createDate, TopicState.namesToStates(stateName), changedBy, changeDate)
    }

    def toRow(topicRecord: TopicRecord) =
      Some((topicRecord.id,
        topicRecord.name,
        topicRecord.description,
        topicRecord.createdBy,
        topicRecord.createDate,
        topicRecord.state.name,
        topicRecord.changedBy,
        topicRecord.changeDate
        ))
  }

  class UserTopicTable(tag:Tag) extends Table[UserTopicRecord](tag,"userTopic") {
    def researcher = column[UserName]("researcher")
    def topicId = column[TopicId]("topicId")
    def state = column[TopicStateName]("state")
    def changedBy = column[UserName]("changedBy")
    def changeDate = column[Date]("changeDate")

    def researcherTopicIdIndex = index("researcherTopicIdIndex",(researcher,topicId),unique = true)

    def * = (researcher, topicId, state, changedBy, changeDate) <> (fromRow, toRow)

    def fromRow = (fromParams _).tupled

    def fromParams(researcher:UserName,
                   topicId:TopicId,
                   stateName:String,
                   changedBy:UserName,
                   changeDate:Date): UserTopicRecord = {
      UserTopicRecord(researcher,topicId,TopicState.namesToStates(stateName), changedBy, changeDate)
    }

    def toRow(userTopicRecord: UserTopicRecord):Option[(UserName,TopicId,String,UserName,Date)] =
      Some((userTopicRecord.researcher,
        userTopicRecord.topicId,
        userTopicRecord.state.name,
        userTopicRecord.changedBy,
        userTopicRecord.changeDate
        ))
  }

  class UserAuditTable(tag:Tag) extends Table[UserAuditRecord](tag,"userAudit") {
    def researcher = column[UserName]("researcher")
    def queryCount = column[Int]("queryCount")
    def changeDate = column[Date]("changeDate")

    def * = (researcher, queryCount, changeDate) <> (fromRow, toRow)

    def fromRow = (fromParams _).tupled

    def fromParams(researcher:UserName,
                   queryCount:Int,
                   changeDate:Date): UserAuditRecord = {
      UserAuditRecord(researcher,queryCount, changeDate)
    }

    def toRow(record: UserAuditRecord):Option[(UserName,Int,Date)] =
      Some((record.researcher,
        record.queryCount,
        record.changeDate
        ))
  }

  class QueryTable(tag:Tag) extends Table[ShrineQueryRecord](tag,"queries") {
    def stewardId = column[StewardQueryId]("stewardId",O.PrimaryKey,O.AutoInc)
    def externalId = column[ExternalQueryId]("id")
    def name = column[String]("name")
    def researcherId = column[UserName]("researcher")
    def topicId = column[Option[TopicId]]("topic")
    def queryContents = column[QueryContents]("queryContents")
    def stewardResponse = column[String]("stewardResponse")
    def date = column[Date]("date")

    def externalIdIndex = index("externalIdIndex",externalId,unique = false)
    def queryNameIndex = index("queryNameIndex",name,unique = false)
    def researcherIdIndex = index("researcherIdIndex",stewardId,unique = false)
    def topicIdIndex = index("topicIdIndex",topicId,unique = false)
    def stewardResponseIndex = index("stewardResponseIndex",stewardResponse,unique = false)
    def dateIndex = index("dateIndex",date,unique = false)

    def * = (stewardId.?,externalId,name,researcherId,topicId,queryContents,stewardResponse,date) <> (fromRow,toRow)

    def fromRow = (fromParams _).tupled

    def fromParams(stewardId:Option[StewardQueryId],
                   externalId:ExternalQueryId,
                   name:String,
                   userId:UserName,
                   topicId:Option[TopicId],
                   queryContents: QueryContents,
                   stewardResponse:String,
                   date:Date): ShrineQueryRecord = {
      ShrineQueryRecord(stewardId,externalId, name, userId, topicId, queryContents,TopicState.namesToStates(stewardResponse),date)
    }
    def toRow(queryRecord: ShrineQueryRecord):Option[(
      Option[StewardQueryId],
        ExternalQueryId,
        String,
        UserName,
        Option[TopicId],
        QueryContents,
        String,
        Date
      )] =
      Some((queryRecord.stewardId,
        queryRecord.externalId,
        queryRecord.name,
        queryRecord.userId,
        queryRecord.topicId,
        queryRecord.queryContents,
        queryRecord.stewardResponse.name,
        queryRecord.date)
      )
  }

  val allUserQuery = TableQuery[UserTable]
  val allTopicQuery = TableQuery[TopicTable]
  val allQueryTable = TableQuery[QueryTable]
  val allUserTopicQuery = TableQuery[UserTopicTable]
  val allUserAudits = TableQuery[UserAuditTable]

  val mostRecentTopicQuery: Query[TopicTable, TopicRecord, Seq] = for(
    topic <- allTopicQuery if !allTopicQuery.filter(_.id === topic.id).filter(_.changeDate > topic.changeDate).exists
  ) yield topic

  val mostRecentUserAudits: Query[UserAuditTable, UserAuditRecord, Seq] = for(
    record <- allUserAudits if !allUserAudits.filter(_.researcher === record.researcher).filter(_.changeDate > record.changeDate).exists
  ) yield record

  val leastRecentUserQuery: Query[QueryTable, ShrineQueryRecord, Seq] = for(
    record <- allQueryTable if !allQueryTable.filter(_.researcherId === record.researcherId).filter(_.date < record.date).exists
  ) yield record

}

object StewardSchema {

  val allConfig:Config = StewardConfigSource.config
  val config:Config = allConfig.getConfig("shrine.steward.database")

  val slickProfile:JdbcProfile = StewardConfigSource.getObject("slickProfileClassName", config)

  val schema = StewardSchema(slickProfile)
}

object StewardDatabase extends NeedsWarmUp {

  val dataSource:DataSource = TestableDataSourceCreator.dataSource(StewardSchema.config)

  val db = StewardDatabase(StewardSchema.schema,dataSource)

  val createTablesOnStart = StewardSchema.config.getBoolean("createTablesOnStart")
  if(createTablesOnStart) StewardDatabase.db.createTables()

  override def warmUp() = StewardDatabase.db.warmUp
}

//API help

sealed case class SortOrder(name:String){
  import slick.lifted.ColumnOrdered

  def orderForColumn[T](column:ColumnOrdered[T]):ColumnOrdered[T] = {
    if(this == SortOrder.ascending) column.asc
    else column.desc
  }
}

object SortOrder {
  val ascending = SortOrder("ascending")
  val descending = SortOrder("descending")

  val sortOrders = Seq(ascending,descending)

  val namesToSortOrders = sortOrders.map(x => (x.name,x)).toMap

  def sortOrderForStringOption(option:Option[String]) = option.fold(ascending)(namesToSortOrders(_))
}

case class QueryParameters(researcherIdOption:Option[UserName] = None,
                            stateOption:Option[TopicState] =  None,
                            skipOption:Option[Int] =  None,
                            limitOption:Option[Int] = None,
                            sortByOption:Option[String] = None,
                            sortOrder:SortOrder = SortOrder.ascending,
                            minDate:Option[Date] = None,
                            maxDate:Option[Date] = None
                          )

//DAO case classes, exposed for testing only
case class ShrineQueryRecord(stewardId: Option[StewardQueryId],
                             externalId:ExternalQueryId,
                             name:String,
                             userId:UserName,
                             topicId:Option[TopicId],
                             queryContents: QueryContents,
                             stewardResponse:TopicState,
                             date:Date) {
  def createOutboundShrineQuery(outboundTopic:Option[OutboundTopic],outboundUser:OutboundUser): OutboundShrineQuery = {
    OutboundShrineQuery(stewardId.get,externalId,name,outboundUser,outboundTopic,queryContents,stewardResponse.name,date)
  }
}

object ShrineQueryRecord extends ((Option[StewardQueryId],ExternalQueryId,String,UserName,Option[TopicId],QueryContents,TopicState,Date) => ShrineQueryRecord) {
  def apply(userId:UserName,topicId:Option[TopicId],shrineQuery: InboundShrineQuery,stewardResponse:TopicState): ShrineQueryRecord = {
    ShrineQueryRecord(
      None,
      shrineQuery.externalId,
      shrineQuery.name,
      userId,
      topicId,
      shrineQuery.queryContents,
      stewardResponse,
      System.currentTimeMillis())
  }
}

case class UserRecord(userName:UserName,fullName:String,isSteward:Boolean) {

  lazy val asOutboundUser:OutboundUser = OutboundUser(userName,fullName,if(isSteward) Set(stewardRole,researcherRole)
                                                                        else Set(researcherRole))
}

object UserRecord extends ((UserName,String,Boolean) => UserRecord) {

  def apply(user:User):UserRecord = UserRecord(user.username,user.fullName,user.params.toList.contains((stewardRole,"true")))

}

case class TopicRecord(id:Option[TopicId] = None,
                        name:String,
                        description:String,
                        createdBy:UserName,
                        createDate:Date,
                        state:TopicState,
                        changedBy:UserName,
                        changeDate:Date) {

  def toOutboundTopic(userNamesToOutboundUsers: Map[UserName, OutboundUser]): OutboundTopic = {
    OutboundTopic(id.get,
      name,
      description,
      userNamesToOutboundUsers(createdBy),
      createDate,
      state.name,
      userNamesToOutboundUsers(changedBy),
      changeDate)
  }
}


object TopicRecord {
  def apply(id:Option[TopicId],
            name:String,
            description:String,
            createdBy:UserName,
            createDate:Date,
            state:TopicState
             ):TopicRecord = TopicRecord(id,
                                          name,
                                          description,
                                          createdBy,
                                          createDate,
                                          state,
                                          createdBy,
                                          createDate)
}

case class UserTopicRecord(researcher:UserName,
                            topicId:TopicId,
                            state:TopicState,
                            changedBy:UserName,
                            changeDate:Date)

case class UserAuditRecord(researcher:UserName,
                           queryCount:Int,
                           changeDate:Date) {
  def sameExceptForTimes(userAuditRecord: UserAuditRecord):Boolean = {
    (researcher == userAuditRecord.researcher) &&
      (queryCount == userAuditRecord.queryCount)
  }
}

case class TopicDoesNotExist(topicId:TopicId) extends IllegalArgumentException(s"No topic for id $topicId")

case class ApprovedTopicCanNotBeChanged(topicId:TopicId) extends IllegalStateException(s"Topic $topicId has been ${TopicState.approved}")

case class DetectedAttemptByWrongUserToChangeTopic(topicId:TopicId,userId:UserName,ownerId:UserName) extends IllegalArgumentException(s"$userId does not own $topicId; $ownerId owns it.")