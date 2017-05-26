package net.shrine.json.store.db

import java.util.UUID
import java.util.concurrent.TimeoutException
import javax.sql.DataSource

import com.typesafe.config.Config
import net.shrine.slick.{CouldNotRunDbIoActionException, NeedsWarmUp, TestableDataSourceCreator}
import net.shrine.source.ConfigSource
import slick.dbio.Effect.Read
import slick.dbio.SuccessAction
import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable
import slick.profile.{FixedSqlStreamingAction, SqlAction}

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Database access to a json store via slick
  *
  * @author david 
  * @since 5/16/17
  */
object JsonStoreDatabase extends NeedsWarmUp {

  val config:Config = ConfigSource.config.getConfig("shrine.jsonStore.database")
  val slickProfile:JdbcProfile = ConfigSource.getObject("slickProfileClassName", config)
  val timeout: Duration = ConfigSource.config.getInt("shrine.problem.timeout").seconds

  import slickProfile.api._

  val dataSource: DataSource = TestableDataSourceCreator.dataSource(config)

  lazy val db = {
    val db = Database.forDataSource(dataSource)
    val createTables: String = "createTablesOnStart"
    if (config.hasPath(createTables) && config.getBoolean(createTables)) {
      Await.ready(db.run(IOActions.createIfNotExists), timeout)
    }
    db
  }

  def warmUp() = DatabaseConnector.runBlocking(ShrineResultsQ.selectAll.take(10).result)

  case class ShrineResultDbEnvelope(
                                     id:UUID,
                                     version:Int,
                                     tableChangeCount:Int,
                                     //todo get shrine version here from the system as a default value
                                     queryId:UUID,
                                     json:String
                                   )

  /**
    * The Results Table.
    */
  class ShrineResultsT(tag: Tag) extends Table[ShrineResultDbEnvelope](tag, ShrineResultsQ.tableName) {
    def id = column[UUID]("id", O.PrimaryKey)
    //def shrineVersion = column[String]("shrineVersion")
    def version = column[Int]("version") //for optimistic locking
    def tableVersion = column[Int]("tableVersion") //for change detection on a table
    def queryId = column[UUID]("queryId") //for the first pass we're asking strictly for query ids
    def json = column[String]("json")
    def * = (id, version, tableVersion, queryId, json) <> (ShrineResultDbEnvelope.tupled, ShrineResultDbEnvelope.unapply)

    //todo indexes
  }

  /**
    * Queries related to the Problems table.
    */
  object ShrineResultsQ extends TableQuery(new ShrineResultsT(_)) {
    /**
      * The table name
      */
    val tableName = "shrineResults"

    /**
      * Equivalent to Select * from Problems;
      */
    val selectAll = this

    def selectLastTableChange = Query(this.map(_.tableVersion).max)

    def selectById(id:UUID) = selectAll.filter{ _.id === id }

//useful queries like "All the changes to results for a subset of queries since table version ..."

    def withParameters(parameters:ShrineResultQueryParameters) = {
      val everything: Query[ShrineResultsT, ShrineResultDbEnvelope, Seq] = selectAll
      val afterTableChange = parameters.afterTableChange.fold(everything){change => everything.filter(_.tableVersion > change)}
      val justTheseQueries = parameters.forQueryIds.fold(afterTableChange){queryIds => afterTableChange.filter(_.queryId.inSet(queryIds))}

      justTheseQueries
    }
  }

  case class ShrineResultQueryParameters(
                                          afterTableChange:Option[Int] = None,
                                          forQueryIds:Option[Set[UUID]] = None, //None interpreted as "all" . todo check set size < 1000 for Oracle safety if oracle is to be supported.
                                          skip:Option[Int] =  None,
                                          limit:Option[Int] = None
                                        )

  /**
    * DBIO Actions. These are pre-defined IO actions that may be useful.
    * Using it to centralize the location of DBIOs.
    */
  object IOActions {
    // For SuccessAction, just a no_op.
    case object NoOperation

    val tableExists = MTable.getTables(ShrineResultsQ.tableName).map(_.nonEmpty)
    val createIfNotExists = tableExists.flatMap(
      if (_) SuccessAction(NoOperation) else ShrineResultsQ.schema.create)
    val dropIfExists = tableExists.flatMap(
      if (_) ShrineResultsQ.schema.drop else SuccessAction(NoOperation))
    val clearTable = createIfNotExists andThen ShrineResultsQ.selectAll.delete

    val selectAll = ShrineResultsQ.result

    def selectById(id:UUID) = ShrineResultsQ.selectById(id).result.headOption

    def countWithParameters(parameters: ShrineResultQueryParameters) = ShrineResultsQ.withParameters(parameters).size.result

    def selectResultsWithParameters(parameters: ShrineResultQueryParameters) = {
      val select = ShrineResultsQ.withParameters(parameters).sortBy(_.tableVersion.desc) //newest changes first
      val skipSelect = parameters.skip.fold(select){ skip =>
        select.drop(skip)
      }
      val limitSelect = parameters.limit.fold(skipSelect){ limit => skipSelect.take(limit)}

      limitSelect.result
    }

    def selectLastTableChange = ShrineResultsQ.selectLastTableChange.result

    def upsertShrineResult(shrineResult:ShrineResultDbEnvelope) = ShrineResultsQ.insertOrUpdate(shrineResult)

    def insertShrineResults(shrineResults:Seq[ShrineResultDbEnvelope]) = ShrineResultsQ ++= shrineResults

    //todo take a ShrineResult json wrapper trait instead and build up the envelope
    def putShrineResult(shrineResultDbEnvelope: ShrineResultDbEnvelope) = {
      // get the record from the table if it is there
      selectById(shrineResultDbEnvelope.id).flatMap { (storedRecordOption: Option[ShrineResultDbEnvelope]) =>
        //check the version number vs the one you have

        storedRecordOption.fold(){row =>
          if (row.version != shrineResultDbEnvelope.version) throw new IllegalStateException("Stale data in optimistic transaction")}
        //todo better exception

        // get the last table change number
        selectLastTableChange.head.flatMap { (lastTableChangeOption: Option[Int]) =>
          val nextTableChange = lastTableChangeOption.getOrElse(0) + 1
          val newRecord = shrineResultDbEnvelope.copy(
            tableChangeCount = nextTableChange,
            version = storedRecordOption.fold(1)(row => row.version + 1)
          )
          // upsert the query result
          upsertShrineResult(newRecord)
        }
      }
    }
  }

  /**
    * Entry point for interacting with the database. Runs IO actions.
    */
  object DatabaseConnector {

    /**
      * Straight copy of db.run
      */
    def run[R](dbio: DBIOAction[R, NoStream, _]): Future[R] = {
      db.run(dbio)
    }

    /**
      * Synchronized copy of db.run
      */
    def runBlocking[R](dbio: DBIOAction[R, NoStream, _], timeout: Duration = timeout): R = {
      try {
        Await.result(this.run(dbio), timeout)
      } catch {
        case tx:TimeoutException => throw CouldNotRunDbIoActionException(JsonStoreDatabase.dataSource, tx)
          //todo catch better exception here, and rethrow
        case NonFatal(x) => throw CouldNotRunDbIoActionException(JsonStoreDatabase.dataSource, x)
      }
    }

    /**
      * Straight copy of db.run
      */
    def runTransaction[R](dbio: DBIOAction[R, NoStream, _]): Future[R] = {
      db.run(dbio.transactionally)
    }

    /**
      * Synchronized copy of db.run
      */
    def runTransactionBlocking[R](dbio: DBIOAction[R, NoStream, _], timeout: Duration = timeout): R = {
      try {
        Await.result(this.run(dbio.transactionally), timeout)
      } catch {
        case tx:TimeoutException => throw CouldNotRunDbIoActionException(JsonStoreDatabase.dataSource, tx)
        //todo catch better exception here, and rethrow
        case NonFatal(x) => throw CouldNotRunDbIoActionException(JsonStoreDatabase.dataSource, x)
      }
    }
  }
}