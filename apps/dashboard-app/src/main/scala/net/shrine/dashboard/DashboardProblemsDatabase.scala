package net.shrine.dashboard
import java.util.concurrent.TimeoutException
import javax.sql.DataSource

import com.typesafe.config.Config
import net.shrine.config.ConfigExtensions
import net.shrine.problem.ProblemDigest
import net.shrine.slick.{CouldNotRunDbIoActionException, TestableDataSourceCreator}
import slick.dbio.SuccessAction
import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.xml.XML
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Handles all interaction with the problem database. Abstracts away
  * the need for a
  * @author ty
  * @since 07/16
  */
case class ProblemDatabaseConnector() {
  import Problems.slickProfile.api._
  val problems = Problems

  val db = problems.db
  val IO = problems.IOActions
  val Queries = problems.Queries

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
  def runBlocking[R](dbio: DBIOAction[R, NoStream, _])(implicit timeout: Duration): R = {
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
  def insertProblem(problem: ProblemDigest): Unit = {
    run(IO.problems += problem)
  }
}

/**
  * Problems schema object, defines the PROBLEMS table schema and related queries
  */
object Problems {
  val config:Config = DashboardConfigSource.config.getConfig("shrine.dashboard.database")
  val slickProfileClassName = config.getString("slickProfileClassName")
  // TODO: Can we not pay this 2 second cost here?
  val slickProfile:JdbcProfile = DashboardConfigSource.objectForName(slickProfileClassName)

  import slickProfile.api._

  val dataSource: DataSource = TestableDataSourceCreator.dataSource(config)
  lazy val db = {
    val db = Database.forDataSource(dataSource)
    if (config.getBoolean("createTablesOnStart"))
      Await.ready(db.run(IOActions.createIfNotExists), FiniteDuration(3, SECONDS))
    db
  }

  /**
    * The Problems Table. This is the table schema.
    */
  //TODO: ADD EPOCH VALUE
  class ProblemsT(tag: Tag) extends Table[ProblemDigest](tag, Queries.tableName) {
    def codec = column[String]("codec")
    def stampText = column[String]("stampText")
    def summary = column[String]("summary")
    def description = column[String]("description")
    def xml = column[String]("detailsXml")
    def epoch= column[Long]("epoch")
    // projection between table row and problem digest
    def * = (codec, stampText, summary, description, xml, epoch) <> (rowToProblem, problemToRow)
    def idx = index("idx_epoch", epoch, unique=false)

    /**
      * Converts a table row into a ProblemDigest.
      * @param args the table row, represented as a five-tuple string
      * @return the corresponding ProblemDigest
      */
    def rowToProblem(args: (String, String, String, String, String, Long)): ProblemDigest = args match {
      case (codec, stampText, summary, description, detailsXml, epoch) =>
        ProblemDigest(codec, stampText, summary, description, XML.loadString(detailsXml), epoch)
    }

    /**
      * Converts a ProblemDigest into an Option of a table row. For now there is no failure
      * condition, ie a ProblemDigest can always be a table row, but this is a place for
      * possible future error handling
      * @param problem the ProblemDigest to convert
      * @return an Option of a table row.
      */
    def problemToRow(problem: ProblemDigest): Option[(String, String, String, String, String, Long)] = problem match {
      case ProblemDigest(codec, stampText, summary, description, detailsXml, epoch) =>
        Some((codec, stampText, summary, description, detailsXml.toString, epoch))
    }
  }

  /**
    * Queries related to the Problems table.
    */
  object Queries extends TableQuery(new ProblemsT(_)) {
    /**
      * The table name
      */
    val tableName = "PROBLEMS"

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
    def sizeAndProblemDigest(n: Int, offset: Int = 0) = for {
      length <- problems.length.result
      allProblems <- problems.lastNProblems(n, offset).result
    } yield (allProblems, length)
  }

}

// For SuccessAction, just a no_op.
case object NoOperation