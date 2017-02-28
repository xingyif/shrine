package net.shrine.json
import java.io.File
import java.util.UUID

import jawn.Parser.parseFromString
import rapture.json._
import rapture.json.jsonBackends.jawn._
import slick.driver.SQLiteDriver.api._
import formatters.compact._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * @by ty
  * @since 2/22/17
  */
class StorageDemo {}

/**
  * @author ty
  */
object Storage {

  def main(args: Array[String]): Unit = {
    val path = "commons/util/src/test/resources/test.db"
    val f = new File(path)
    if (f.exists) {
      f.delete
    }

    val db = Database.forURL(s"jdbc:sqlite:$path", driver = "org.sqlite.JDBC")
    val schema = queryRunsQuery.schema ++
        topicsQuery.schema ++
        queryResultsQuery.schema ++
        usersQuery.schema ++
        adaptersQuery.schema ++
        queriesQuery.schema
  val uid       = UUID.randomUUID()
  val startTime = System.currentTimeMillis()
  val i2b2Xml = <ami2b2>suchi2b2, wow</ami2b2>
  val extraXml = <extra><extra2><extra3>lots of extra</extra3></extra2></extra>
  val noiseTerms = NoiseTerms(10, 11, 12)
  val resultCount = 1000
  val j = Json(Topic("hey", "hey", uid))
  val breakdown = Breakdown("gender", List(BreakdownProperty("male", 70), BreakdownProperty("female", 30)))
  val flags = List("hey", "what's", "that", "sound")
  val queryJson =
    json"""{
              "queryId": $uid,
              "topicId": $uid,
              "userId": $uid,
              "startTime": $startTime,
              "i2b2QueryText": ${i2b2Xml.toString},
              "extraXml": ${extraXml.toString},
              "queryResults": [ $uid ]
           }
    """
  val queryResultJson =
    json"""{
             "status": "success",
             "resultId": $uid,
             "adapterId": $uid,
             "queryId": $uid,
             "count": $resultCount,
             "noiseTerms": {
                "sigma": ${noiseTerms.sigma},
                "clamp": ${noiseTerms.clamp},
                "rounding": ${noiseTerms.rounding}
             },
             "i2b2Mapping": ${i2b2Xml.toString},
             "flags": $flags,
             "breakdowns": [
               {
                 "category": "gender",
                 "results": [
                   {
                     "name": "male",
                     "count": 70
                   },
                   {
                     "name": "female",
                     "count": 30
                   }
                 ]
               }
             ]
          }
    """
    val query = Query(queryJson).get
    val queryResult = QueryResult(queryResultJson).get
    val user = User("user", "domain", uid)
    val topic = Topic("topic", "domain", uid)
    val adapter = Adapter("adapter", uid)
    val drop = schema.drop
    val setup = schema.create

    val jq = json"""{"q": {"q2": 20}, "epoch": 12842184}"""
    val insertQuery = insert(query, user, topic, adapter, queryResult) match {
      case Left(s) => throw new IllegalArgumentException(s)
      case Right(dbQuery) => dbQuery
    }

    val results = Await.result(db.run(for {
      _ <- setup
      _ <- insertQuery
      a <- queriesQuery.result
    } yield a), 10.seconds)
    println(results)
  }

  def insert(query: Query,
             user: User,
             topic: Topic,
             adapter: Adapter,
             result: QueryResult) = {
    if (result.queryId != query.queryId)
      Left("The result's queryId does not match the query's queryId")
    else if (query.topicId != topic.id)
      Left("The query's topicId does not match the topic's id")
    else if (query.userId != user.id)
      Left("The query's userId does not match the user's id")
    else if (result.adapterId != adapter.id)
      Left("The query result's adapterId does not match the adapter's id")
    else
      Right(DBIO.seq(
        queriesQuery += query,
        queryResultsQuery += result,
        usersQuery += user,
        topicsQuery += topic,
        adaptersQuery += adapter,
        queryRunsQuery += (UUID.randomUUID(), query.queryId, result.resultId, user.id, topic.id, adapter.id)))
  }

  class Queries(tag: Tag) extends Table[Query](tag, "QUERIES") {
    def queryId = column[UUID]("query_id", O.PrimaryKey)

    def queryDate = column[Long]("query_epoch")

    def queryJson = column[String]("query_json")

    def * = (queryId, queryDate, queryJson) <> (
      (row: (UUID, Long, String)) => Json(parseFromString(row._3).get).as[Query],
      (query: Query) => Some((query.queryId, query.startTime, query.json.toBareString))
    )
  }
  val queriesQuery = TableQuery[Queries]

  class QueryResults(tag: Tag)
      extends Table[QueryResult](tag, "QUERYRESULTS") {
    def queryResultId = column[UUID]("query_result_id", O.PrimaryKey)

    def queryResultJson = column[String]("query_result_json")

    def * = (queryResultId, queryResultJson) <> (
      (row: (UUID, String)) => Json(parseFromString(row._2).get).as[QueryResult],
      (result: QueryResult) => Some((result.resultId, result.json.toBareString)))
  }
  val queryResultsQuery = TableQuery[QueryResults]

  class Users(tag: Tag) extends Table[User](tag, "USERS") {

    def userId = column[UUID]("user_id", O.PrimaryKey)

    def userJson = column[String]("user_json")


    def * = {
      (userId, userJson) <> (
        (row: (UUID, String)) => Json(parseFromString(row._2).get).as[User],
        (user: User) => Some((user.id, Json(user).toBareString)))
    }
  }
  val usersQuery = TableQuery[Users]

  class Topics(tag: Tag) extends Table[Topic](tag, "TOPICS") {
    def topicId = column[UUID]("topic_id", O.PrimaryKey)

    def topicJson = column[String]("topic_json")

    def * = (topicId, topicJson) <> (
      (row: (UUID, String)) => Json(jawn.Parser.parseFromString(row._2).get).as[Topic],
      (topic: Topic) => Some((topic.id, Json(topic).toBareString)))
  }
  val topicsQuery = TableQuery[Topics]

  class Adapters(tag: Tag) extends Table[Adapter](tag, "ADAPTERS") {
    def adapterId = column[UUID]("adapter_id", O.PrimaryKey)

    def adapterJson = column[String]("adapter_json")

    def * = (adapterId, adapterJson) <> (
      (row: (UUID, String)) => Json(jawn.Parser.parseFromString(row._2).get).as[Adapter],
      (adapter: Adapter) => Some((adapter.id, Json(adapter).toBareString)))
  }
  val adaptersQuery = TableQuery[Adapters]

  class QueryRuns(tag: Tag)
      extends Table[(UUID, UUID, UUID, UUID, UUID, UUID)](tag, "QUERYRUNS") {
    def queryRunId = column[UUID]("query_run_id", O.PrimaryKey)

    def queryId = column[UUID]("query_id")

    def queryResultId = column[UUID]("query_result_id")

    def userId = column[UUID]("user_id")

    def topicId = column[UUID]("topic_id")

    def adapterId = column[UUID]("adapter_id")

    def * = (queryRunId, queryId, queryResultId, userId, topicId, adapterId)

    def queryFk =
      foreignKey("query_fk", queryId, queriesQuery)(
        _.queryId,
        onUpdate = ForeignKeyAction.NoAction,
        onDelete = ForeignKeyAction.NoAction)

    def queryResultFk =
      foreignKey("query_result_fk", queryResultId, queryResultsQuery)(
        _.queryResultId,
        onUpdate = ForeignKeyAction.NoAction,
        onDelete = ForeignKeyAction.NoAction)

    def userFk =
      foreignKey("user_fk", userId, usersQuery)(
        _.userId,
        onUpdate = ForeignKeyAction.NoAction,
        onDelete = ForeignKeyAction.NoAction)

    def topicFk =
      foreignKey("topic_fk", topicId, topicsQuery)(
        _.topicId,
        onUpdate = ForeignKeyAction.NoAction,
        onDelete = ForeignKeyAction.NoAction)

    def adapterFk =
      foreignKey("adapter_fk", adapterId, adaptersQuery)(
        _.adapterId,
        onUpdate = ForeignKeyAction.NoAction,
        onDelete = ForeignKeyAction.NoAction)
  }
  val queryRunsQuery = TableQuery[QueryRuns]

}
