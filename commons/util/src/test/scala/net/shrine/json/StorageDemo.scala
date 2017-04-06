package net.shrine.json
import java.io.File
import java.nio.charset.Charset
import java.util.UUID

import com.typesafe.config.ConfigFactory
import jawn.Parser.parseFromString
import rapture.json._
import rapture.json.jsonBackends.jawn._
import slick.dbio.Effect.Write
import slick.driver.{H2Driver, JdbcProfile, MySQLDriver, SQLiteDriver}
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
    val queryResults = (0 to 10).map(i =>
      QueryResult(queryResultJson.copy(_.resultId = UUID.randomUUID()))
        .get)
    val query = Query(queryJson.copy(_.queryResults = queryResults.map(_.resultId))).get
    val user = User("user", "domain", uid)
    val topic = Topic("topic", "domain", uid)
    val adapter = Adapter("adapter", uid)

    val sqliteDB = Database.forConfig("sqlite", ConfigFactory.load("shrine.conf"))
    val h2DB = Database.forConfig("h2", ConfigFactory.load("shrine.conf"))
    val sqliteDAO = DAO(SQLiteDriver)
    val h2DAO = DAO(H2Driver)
    //    Await.result(sqliteDB.run(setup), 10.seconds)
    Seq((sqliteDAO, sqliteDB), (h2DAO, h2DB)).foreach(daodb => {
      val (dao, db) = daodb
      import dao.profile.api._
      val insertQueries = queryResults.map(queryResult =>
        dao.SlickQueries.insert(query, user, topic, adapter, queryResult) match {
          case Left(s) => throw new IllegalArgumentException(s)
          case Right(dbQuery) => dbQuery
      })
      val acts = dao.SlickQueries.create
        .andThen(DBIO.sequence(insertQueries))
        .andThen(dao.queryRunsQuery.result)
        .andThen(sqliteDAO.SlickQueries.selectJsonForQuery(uid))
      val results = Await.result(db.run(acts), 10.seconds)
      println(results)
    })
//    println(h2DAO.SlickQueries.create.statements.mkString(";\n"))
//    println(DAO(MySQLDriver).SlickQueries.create.statements.mkString(";\n"))
  }
}

  case class DAO(profile: JdbcProfile) {
    import profile.api._

    val charset = "UTF-8"
    def bytesToJson(bytes: Array[Byte]): Json = {
      Json(jawn.Parser.parseFromString(new String(bytes, charset)).get)
    }

    def jsonToBytes(json: Json): Array[Byte] = {
      json.toBareString.getBytes(charset)
    }

    class Queries(tag: Tag) extends Table[ShrineQuery](tag, "QUERIES") {
      def queryId = column[UUID]("query_id", O.PrimaryKey)

      def queryDate = column[Long]("query_epoch")

      def queryJson = column[Array[Byte]]("query_json")

      def * = (queryId, queryDate, queryJson) <> (
        (row: (UUID, Long, Array[Byte])) =>
          bytesToJson(row._3).as[ShrineQuery],
        (query: ShrineQuery) =>
          Some((query.queryId, query.startTime, jsonToBytes(query.json)))
      )
    }

    val queries = TableQuery[Queries]

    class QueryResults(tag: Tag)
        extends Table[QueryResult](tag, "QUERYRESULTS") {
      def queryResultId = column[UUID]("query_result_id", O.PrimaryKey)

      def queryResultJson = column[Array[Byte]]("query_result_json")

      def * =
        (queryResultId, queryResultJson) <> ((row: (UUID, Array[Byte])) =>
          bytesToJson(row._2).as[QueryResult],
        (result: QueryResult) =>
          Some((result.resultId, jsonToBytes(result.json))))
    }

    val queryResults = TableQuery[QueryResults]

    class Users(tag: Tag) extends Table[User](tag, "USERS") {

      def userId = column[UUID]("user_id", O.PrimaryKey)

      def userJson = column[Array[Byte]]("user_json")

      def * = {
        (userId, userJson) <> ((row: (UUID, Array[Byte])) =>
          bytesToJson(row._2).as[User],
        (user: User) => Some((user.id, jsonToBytes(Json(user)))))
      }
    }

    val users = TableQuery[Users]

    class Topics(tag: Tag) extends Table[Topic](tag, "TOPICS") {
      def topicId = column[UUID]("topic_id", O.PrimaryKey)

      def topicJson = column[Array[Byte]]("topic_json")

      def * =
        (topicId, topicJson) <> ((row: (UUID, Array[Byte])) =>
          bytesToJson(row._2).as[Topic],
        (topic: Topic) => Some((topic.id, jsonToBytes(Json(topic)))))
    }

    val topics = TableQuery[Topics]

    class Adapters(tag: Tag) extends Table[Adapter](tag, "ADAPTERS") {
      def adapterId = column[UUID]("adapter_id", O.PrimaryKey)

      def adapterJson = column[Array[Byte]]("adapter_json")

      def * =
        (adapterId, adapterJson) <> ((row: (UUID, Array[Byte])) =>
          bytesToJson(row._2).as[Adapter],
        (adapter: Adapter) => Some((adapter.id, jsonToBytes(Json(adapter)))))
    }

    val adapters = TableQuery[Adapters]

    class QueryRuns(tag: Tag)
        extends Table[(Int, UUID, UUID, UUID, UUID, UUID)](tag, "QUERYRUNS") {
      def queryRunId = column[Int]("query_run_id", O.PrimaryKey, O.AutoInc)

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
      def selectAllForQuery(queryId: UUID) =
        for {
          queryRun <- queryRunsQuery.filter(_.queryId === queryId)
          q <- queries if q.queryId === queryRun.queryId
          r <- queryResults if r.queryResultId === queryRun.queryResultId
          u <- users if u.userId === queryRun.userId
          t <- topics if t.topicId === queryRun.topicId
          a <- adapters if a.adapterId === queryRun.adapterId
        } yield(q, r, u, t, a)

      // Generates the json for a given query Id
      def selectJsonForQuery(queryId: UUID) = {
        val allResults = selectAllForQuery(queryId)
        allResults.result.map(t => {
          t.headOption.map{
            case (query, _, user, topic, _) =>
              val queryJson = query.json
              val jb = JsonBuffer.construct(queryJson.$root.copy(), Vector())
              jb -= "userId"
              jb -= "topicId"
              jb.user = user
              jb.topic = topic
              jb.queryResults = t.map{
                case (_, queryResult, _, _, adapter) =>
                  val jb2 = JsonBuffer.construct(queryResult.json.$root.copy(), Vector())
                  jb2 -= "adapterId"
                  jb2.adapter = adapter
                  jb2
              }
              Json(jb)
          }.getOrElse(json"""{}""")
        })
      }

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
              queryRunsQuery += (0, query.queryId, result.resultId, user.id, topic.id, adapter.id)
            ))
    }
      def schema = queries.schema ++ users.schema ++ topics.schema ++ adapters.schema ++ queryResults.schema ++ queryRunsQuery.schema
      def create = schema.create
  }
}


