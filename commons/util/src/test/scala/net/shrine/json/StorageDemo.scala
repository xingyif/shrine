package net.shrine.json
import java.io.File
import java.util.UUID

import jawn.Parser.parseFromString
import rapture.json._
import rapture.json.jsonBackends.jawn._
import slick.dbio.Effect.Write
import slick.driver.{JdbcProfile, SQLiteDriver}
import slick.jdbc.JdbcBackend.Database
import net.shrine.json.{Query => ShrineQuery}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

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
    val db = Database.forConfig("sqlite")
    //forURL(s"jdbc:sqlite::memory:", driver = "org.sqlite.JDBC")
    val dao = DAO(SQLiteDriver)

    val uid = UUID.randomUUID()
    val startTime = System.currentTimeMillis()
    val i2b2Xml = <ami2b2>suchi2b2, wow</ami2b2>
    val extraXml =
      <extra>
        <extra2>
          <extra3>lots of extra</extra3>
        </extra2>
      </extra>
    val noiseTerms = NoiseTerms(10, 11, 12)
    val resultCount = 1000
    val j = Json(Topic("hey", "hey", uid))
    val breakdown = Breakdown(
      "gender",
      List(BreakdownProperty("male", 70), BreakdownProperty("female", 30)))
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
    val query = ShrineQuery(queryJson).get
    val queryResult = QueryResult(queryResultJson).get
    val user = User("user", "domain", uid)
    val topic = Topic("topic", "domain", uid)
    val adapter = Adapter("adapter", uid)

    val jq = json"""{"q": {"q2": 20}, "epoch": 12842184}"""
    val insertQuery =
      dao.SlickQueries.insert(query, user, topic, adapter, queryResult) match {
        case Left(s)        => throw new IllegalArgumentException(s)
        case Right(dbQuery) => dbQuery
      }
    //    Await.result(db.run(setup), 10.seconds)
    import dao.profile.api._
    val acts = dao.SlickQueries.create
      .andThen(insertQuery)
      .andThen(dao.queryRunsQuery.result)
    val results = Await.result(db.run(acts), 10.seconds)
    println(results)
  }
}

  case class DAO(profile: JdbcProfile) {
    import profile.api._

    class Queries(tag: Tag) extends Table[ShrineQuery](tag, "QUERIES") {
      def queryId = column[UUID]("query_id", O.PrimaryKey)

      def queryDate = column[Long]("query_epoch")

      def queryJson = column[String]("query_json")

      def * = (queryId, queryDate, queryJson) <> (
        (row: (UUID, Long, String)) =>
          Json(parseFromString(row._3).get).as[ShrineQuery],
        (query: ShrineQuery) =>
          Some((query.queryId, query.startTime, query.json.toBareString))
      )
    }

    val queries = TableQuery[Queries]

    class QueryResults(tag: Tag)
        extends Table[QueryResult](tag, "QUERYRESULTS") {
      def queryResultId = column[UUID]("query_result_id", O.PrimaryKey)

      def queryResultJson = column[String]("query_result_json")

      def * =
        (queryResultId, queryResultJson) <> ((row: (UUID, String)) =>
          Json(parseFromString(row._2).get).as[QueryResult],
        (result: QueryResult) =>
          Some((result.resultId, result.json.toBareString)))
    }

    val queryResults = TableQuery[QueryResults]

    class Users(tag: Tag) extends Table[User](tag, "USERS") {

      def userId = column[UUID]("user_id", O.PrimaryKey)

      def userJson = column[String]("user_json")

      def * = {
        (userId, userJson) <> ((row: (UUID, String)) =>
          Json(parseFromString(row._2).get).as[User],
        (user: User) => Some((user.id, Json(user).toBareString)))
      }
    }

    val users = TableQuery[Users]

    class Topics(tag: Tag) extends Table[Topic](tag, "TOPICS") {
      def topicId = column[UUID]("topic_id", O.PrimaryKey)

      def topicJson = column[String]("topic_json")

      def * =
        (topicId, topicJson) <> ((row: (UUID, String)) =>
          Json(jawn.Parser.parseFromString(row._2).get).as[Topic],
        (topic: Topic) => Some((topic.id, Json(topic).toBareString)))
    }

    val topics = TableQuery[Topics]

    class Adapters(tag: Tag) extends Table[Adapter](tag, "ADAPTERS") {
      def adapterId = column[UUID]("adapter_id", O.PrimaryKey)

      def adapterJson = column[String]("adapter_json")

      def * =
        (adapterId, adapterJson) <> ((row: (UUID, String)) =>
          Json(jawn.Parser.parseFromString(row._2).get).as[Adapter],
        (adapter: Adapter) => Some((adapter.id, Json(adapter).toBareString)))
    }

    val adapters = TableQuery[Adapters]

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
        foreignKey("query_fk", queryId, queries)(
          _.queryId,
          onUpdate = ForeignKeyAction.NoAction,
          onDelete = ForeignKeyAction.NoAction)

      def queryResultFk =
        foreignKey("query_result_fk", queryResultId, queryResults)(
          _.queryResultId,
          onUpdate = ForeignKeyAction.NoAction,
          onDelete = ForeignKeyAction.NoAction)

      def userFk =
        foreignKey("user_fk", userId, users)(
          _.userId,
          onUpdate = ForeignKeyAction.NoAction,
          onDelete = ForeignKeyAction.NoAction)

      def topicFk =
        foreignKey("topic_fk", topicId, topics)(
          _.topicId,
          onUpdate = ForeignKeyAction.NoAction,
          onDelete = ForeignKeyAction.NoAction)

      def adapterFk =
        foreignKey("adapter_fk", adapterId, adapters)(
          _.adapterId,
          onUpdate = ForeignKeyAction.NoAction,
          onDelete = ForeignKeyAction.NoAction)
    }

    val queryRunsQuery = TableQuery[QueryRuns]

    object SlickQueries {
//      def selectAllForQuery(queryId: UUID): Query[QueryRuns, (UUID, UUID, UUID, UUID, UUID, UUID), Seq] =
//        for {
//          (qid, rid, uid, tid, aid) <- queryRunsQuery.filter(_.queryId === queryId)
//          (q, r, u, t, a) <- queries join users on (_.queryId === queryId && _.userId === uid)
//        }

      def insert(query: ShrineQuery,
                 user: User,
                 topic: Topic,
                 adapter: Adapter,
                 result: QueryResult)
      : Either[String, DBIOAction[Unit, NoStream, Write]] = {
        if (result.queryId != query.queryId)
          Left("The result's queryId does not match the query's queryId")
        else if (query.topicId != topic.id)
          Left("The query's topicId does not match the topic's id")
        else if (query.userId != user.id)
          Left("The query's userId does not match the user's id")
        else if (result.adapterId != adapter.id)
          Left("The query result's adapterId does not match the adapter's id")
        else
          Right(
            DBIO.seq(
              // Have to upsert here, as these items may already be in the table
              queries.insertOrUpdate(query),
              queryResults.insertOrUpdate(result),
              users.insertOrUpdate(user),
              topics.insertOrUpdate(topic),
              adapters.insertOrUpdate(adapter),
              queryRunsQuery += (UUID
                .randomUUID(), query.queryId, result.resultId, user.id, topic.id, adapter.id)
            ))
    }
      def schema = queries.schema ++ users.schema ++ topics.schema ++ adapters.schema ++ queryResults.schema ++ queryRunsQuery.schema
      def create = schema.create
  }
}


