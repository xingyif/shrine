package net.shrine.dashboard
import java.util.concurrent.TimeoutException
import javax.sql.DataSource

import com.typesafe.config.Config
import net.shrine.problem.ProblemDigest
import net.shrine.slick.TestableDataSourceCreator
import slick.dbio.SuccessAction
import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.xml.XML
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Handles all interaction with the (current) problem database. Will eventually
  * need to be set up to be able to handle starting up with a config file
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
      case tx:TimeoutException => throw CouldNotRunDbIoActionException(tx)
      case NonFatal(x) => throw CouldNotRunDbIoActionException(x)
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
      case tx:TimeoutException => throw CouldNotRunDbIoActionException(tx)
      case NonFatal(x) => throw CouldNotRunDbIoActionException(x)
    }
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
  class ProblemsT(tag: Tag) extends Table[ProblemDigest](tag, Queries.tableName) {
    def codec = column[String]("codec")
    def stampText = column[String]("stampText", O.PrimaryKey)
    def summary = column[String]("summary")
    def description = column[String]("description")
    def xml = column[String]("detailsXml")
    // projection between table row and problem digest
    def * = (codec, stampText, summary, description, xml) <> (rowToProblem, problemToRow)

    /**
      * Converts a table row into a ProblemDigest.
      * @param args the table row, represented as a five-tuple string
      * @return the corresponding ProblemDigest
      */
    def rowToProblem(args: (String, String, String, String, String)): ProblemDigest = args match {
      case (codec, stampText, summary, description, detailsXml) =>
        ProblemDigest(codec, stampText, summary, description, XML.loadString(detailsXml))
    }

    /**
      * Converts a ProblemDigest into an Option of a table row. For now there is no failure
      * condition, ie a ProblemDigest can always be a table row, but this is a place for
      * possible future error handling
      * @param problem the ProblemDigest to convert
      * @return an Option of a table row.
      */
    def problemToRow(problem: ProblemDigest): Option[(String, String, String, String, String)] = {
      Some((problem.codec, problem.stampText, problem.summary, problem.description, problem.detailsXml.toString))
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
    def lastNProblems(n: Int)(offset: Int) = this.sortBy(_.stampText.desc).drop(offset).take(n)
  }


  /**
    * DBIO Actions. These are pre-defined IO actions that may be useful.
    * Using it to centralize the location of DBIOs. This may be temporary
    * if I can figure out a better solution to the slick.H2.driver._ import
    * issue.
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
    def sizeAndProblemDigest(n: Int, offset: Int) = for {
      length <- problems.length.result
      allProblems <- problems.lastNProblems(n)(offset).result
    } yield (allProblems, length)
  }

}

/**
  * Copy of Dave's DbIoActionException
  */
case class CouldNotRunDbIoActionException(exception: Throwable) extends RuntimeException(exception) {
  //TODO: Datasource
  override def getMessage:String = s"Could not use the database due to ${exception.getLocalizedMessage}"
}

case object NoOperation