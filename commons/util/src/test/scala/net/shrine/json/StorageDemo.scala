package net.shrine.json
import java.io.File

import slick.driver.SQLiteDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import rapture.json._
import jsonBackends.jawn._
import java.util.UUID

import jawn.Parser.parseFromString

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Success, Try}

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

    val drop = schema.drop
    val setup = schema.create

    val jq = json"""{"q": {"q2": 20}, "epoch": 12842184}"""

    val results = Await.result(db.run(for {
      _ <- setup
      a <- queriesQuery.result
    } yield a), 10.seconds)
    println(results)
  }

  def insert(query: Query,
             user: User,
             topic: Topic,
             adapter: Adapter,
             result: QueryResult,
             db: Database): Option[Future[Unit]] = {
    if (result.queryId == query.queryId &&
      query.topicId == topic.id &&
      query.userId == user.id &&
      result.adapterId == adapter.id) {
      val seq = DBIO.seq(
        queriesQuery += query,
        queryResultsQuery += result,
        usersQuery += user,
        topicsQuery += topic,
        adaptersQuery += adapter,
        queryRunsQuery += (UUID.randomUUID(), query.queryId, result.resultId, user.id, topic.id, adapter.id))
      Some(db.run(seq))
    } else {
      None
    }
  }

  class Queries(tag: Tag) extends Table[Query](tag, "QUERIES") {
    def queryId = column[UUID]("query_id", O.PrimaryKey)

    def queryDate = column[Long]("query_epoch")

    def queryJson = column[String]("query_json")

    def * = (queryId, queryDate, queryJson) <> (
      (row: (UUID, Long, String)) => Json(parseFromString(row._3)).as[Query],
      (query: Query) => Some((query.queryId, query.startTime, Json.format(query)))
    )
  }
  val queriesQuery = TableQuery[Queries]

  class QueryResults(tag: Tag)
      extends Table[QueryResult](tag, "QUERYRESULTS") {
    def queryResultId = column[UUID]("query_result_id", O.PrimaryKey)

    def queryResultJson = column[String]("query_result_json")

    def * = (queryResultId, queryResultJson) <> (
      (row: (UUID, String)) => Json(parseFromString(row._2)).as[QueryResult],
      (result: QueryResult) => Some((result.resultId, Json.format(result))))
  }
  val queryResultsQuery = TableQuery[QueryResults]

  class Users(tag: Tag) extends Table[User](tag, "USERS") {
    def userId = column[UUID]("user_id", O.PrimaryKey)

    def userJson = column[String]("user_json")

    def * = (userId, userJson) <> (
      (row: (UUID, String)) => Json(parseFromString(row._2)).as[User],
      (user: User) => Some((user.id, Json.format(user))))
  }
  val usersQuery = TableQuery[Users]

  class Topics(tag: Tag) extends Table[Topic](tag, "TOPICS") {
    def topicId = column[UUID]("topic_id", O.PrimaryKey)

    def topicJson = column[String]("topic_json")

    def * = (topicId, topicJson) <> (
      (row: (UUID, String)) => Json(jawn.Parser.parseFromString(row._2)).as[Topic],
      (topic: Topic) => Some((topic.id, Json.format(topic))))
  }
  val topicsQuery = TableQuery[Topics]

  class Adapters(tag: Tag) extends Table[Adapter](tag, "ADAPTERS") {
    def adapterId = column[UUID]("adapter_id", O.PrimaryKey)

    def adapterJson = column[String]("adapter_json")

    def * = (adapterId, adapterJson) <> (
      (row: (UUID, String)) => Json(jawn.Parser.parseFromString(row._2)).as[Adapter],
      (adapter: Adapter) => Some((adapter.id, Json.format(adapter))))
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
