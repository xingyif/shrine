package net.shrine.problem

import java.sql.Clob
import java.util.concurrent.TimeoutException
import javax.sql.DataSource

import com.typesafe.config.Config
import net.shrine.slick.{CouldNotRunDbIoActionException, TestableDataSourceCreator}
import slick.dbio.SuccessAction
import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal
import scala.xml.XML

/**
  * Problems database object, defines the PROBLEMS table schema and related queries,
  * as well as all interactions with the database.
  * @author ty
  * @since 07/16
  */
object Problems {
  val config:Config = ProblemConfigSource.config.getConfig("shrine.dashboard.database")
  val slickProfileClassName = config.getString("slickProfileClassName")
  // TODO: Can we not pay this 2 second cost here?
  val slickProfile:JdbcProfile = ProblemConfigSource.objectForName(slickProfileClassName)

  import slickProfile.api._

  val dataSource: DataSource = TestableDataSourceCreator.dataSource(config)

  lazy val db = {
    val db = Database.forDataSource(dataSource)
    val createTables: String = "createTablesOnStart"
    if (config.hasPath(createTables) && config.getBoolean(createTables)) {
      val duration = FiniteDuration(3, SECONDS)
      Await.ready(db.run(IOActions.createIfNotExists), duration)
      val testValues: String = "createTestValuesOnStart"
      if (config.hasPath(testValues) && config.getBoolean(testValues)) {
        def dumb(id: Int) = ProblemDigest(s"codec($id)", s"stamp($id)", s"sum($id)", s"desc($id)", <details>{id}</details>, id)
        val dummyValues: Seq[ProblemDigest] = Seq(0, 1, 2, 3, 4, 5, 6).map(dumb)
        Await.ready(db.run(Queries ++= dummyValues), duration)
      }
    }

    db
  }

  /**
    * The Problems Table. This is the table schema.
    */
  class ProblemsT(tag: Tag) extends Table[ProblemDigest](tag, Queries.tableName) {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def codec = column[String]("codec")
    def stampText = column[String]("stampText")
    def summary = column[String]("summary")
    def description = column[String]("description")
    def xml = column[String]("detailsXml", O.SqlType("Clob"))
    def epoch= column[Long]("epoch")
    // projection between table row and problem digest
    def * = (id, codec, stampText, summary, description, xml, epoch) <> (rowToProblem, problemToRow)
    def idx = index("idx_epoch", epoch, unique=false)

    /**
      * Converts a table row into a ProblemDigest.
      * @param args the table row, represented as a five-tuple string
      * @return the corresponding ProblemDigest
      */
    def rowToProblem(args: (Int, String, String, String, String, String, Long)): ProblemDigest = args match {
      case (id, codec, stampText, summary, description, detailsXml, epoch) =>
        ProblemDigest(codec, stampText, summary, description, XML.loadString(detailsXml), epoch)
    }

    /**
      * Converts a ProblemDigest into an Option of a table row. For now there is no failure
      * condition, ie a ProblemDigest can always be a table row, but this is a place for
      * possible future error handling
      * @param problem the ProblemDigest to convert
      * @return an Option of a table row.
      */
    def problemToRow(problem: ProblemDigest): Option[(Int, String, String, String, String, String, Long)] = problem match {
      case ProblemDigest(codec, stampText, summary, description, detailsXml, epoch) =>
        // 0 is ignored on insert and replaced with an auto incremented id
        Some((7, codec, stampText, summary, description, detailsXml.toString, epoch))
    }
  }

  /**
    * Queries related to the Problems table.
    */
  object Queries extends TableQuery(new ProblemsT(_)) {
    /**
      * The table name
      */
    val tableName = "problems"

    /**
      * Equivalent to Select * from Problems;
      */
    val selectAll = this

    /**
      * Selects all the details xml sorted by the problem's time stamp.
      */
    val selectDetails = this.map(_.xml)

    /**
      * Selects the last N problems, after the offset
      */
    def lastNProblems(n: Int, offset: Int = 0) = this.sortBy(_.epoch.desc).drop(offset).take(n)
  }


  /**
    * DBIO Actions. These are pre-defined IO actions that may be useful.
    * Using it to centralize the location of DBIOs.
    */
  object IOActions {
    val problems = Queries
    val tableExists = MTable.getTables(problems.tableName).map(_.nonEmpty)
    val createIfNotExists = tableExists.flatMap(
      if (_) SuccessAction(NoOperation) else problems.schema.create)
    val dropIfExists = tableExists.flatMap(
      if (_) problems.schema.drop else SuccessAction(NoOperation))
    val resetTable = createIfNotExists >> problems.selectAll.delete
    val selectAll = problems.result
    def sizeAndProblemDigest(n: Int, offset: Int = 0) = problems.lastNProblems(n, offset).result.zip(problems.size.result)
  }


  /**
    * Entry point for interacting with the database. Runs IO actions.
    */
  object DatabaseConnector {
    val IO = IOActions
    /**
      * Executes a series of IO actions as a single transactions
      */
    def executeTransaction(actions: DBIOAction[_, NoStream, _]*): Future[Unit] = {
      db.run(DBIO.seq(actions:_*).transactionally)
    }

    /**
      * Executes a series of IO actions as a single transaction, synchronous
      */
    def executeTransactionBlocking(actions: DBIOAction[_, NoStream, _]*)(implicit timeout: Duration): Unit = {
      try {
        Await.ready(this.executeTransaction(actions: _*), timeout)
      } catch {
        // TODO: Handle this better
        case tx:TimeoutException => throw CouldNotRunDbIoActionException(Problems.dataSource, tx)
        case NonFatal(x) => throw CouldNotRunDbIoActionException(Problems.dataSource, x)
      }
    }

    /**
      * Straight copy of db.run
      */
    def run[R](dbio: DBIOAction[R, NoStream, _]): Future[R] = {
      db.run(dbio)
    }

    /**
      * Synchronized copy of db.run
      */
    def runBlocking[R](dbio: DBIOAction[R, NoStream, _])(implicit timeout: Duration = new FiniteDuration(15, SECONDS)): R = {
      try {
        Await.result(this.run(dbio), timeout)
      } catch {
        case tx:TimeoutException => throw CouldNotRunDbIoActionException(Problems.dataSource, tx)
        case NonFatal(x) => throw CouldNotRunDbIoActionException(Problems.dataSource, x)
      }
    }

    /**
      * Inserts a problem into the database
      * @param problem the ProblemDigest
      */
    def insertProblem(problem: ProblemDigest) = {
      run(Queries += problem)
    }
  }
}


// For SuccessAction, just a no_op.
case object NoOperation