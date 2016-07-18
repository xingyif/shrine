package net.shrine.dashboard
import java.util.concurrent.TimeoutException

import net.shrine.problem.ProblemDigest
import slick.dbio.SuccessAction
import slick.lifted.{TableQuery, Tag}
import slick.driver.H2Driver.api._
import slick.jdbc.meta.MTable

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.xml.XML

/**
  * Handles all interaction with the (current) problem database. Will eventually
  * need to be set up to be able to handle starting up with a config file
  */
case class ProblemDatabaseConnector(url: DBUrls.URL) {
  import Problems._
  val db = DBUrls.connectWithUrl(url)

  /**
    * DBIO Actions. These are pre-defined IO actions that may be useful
    */
  val tableExists = MTable.getTables(tableName).map(_.nonEmpty)
  val createIfNotExists = tableExists.map(if (_) SuccessAction else Queries.schema.create)
  val dropIfExists = tableExists.map(if (_) Queries.schema.drop else SuccessAction)
  val resetTable = createIfNotExists >> Queries.selectAll.delete

  /**
    * Executes a series of IO actions as a single transactions
    */
  def executeTransaction(actions: DBIOAction[_, NoStream, _]*): Future[Unit] = {
      db.run(DBIO.seq(actions:_*).transactionally)
    }

  /**
    * Executes a series of IO actions as a single transaction, synchronous
    */
  def executeTransactionBlocking(actions: DBIOAction[_, NoStream, _]*): Unit = {
    try {
      blocking {
        Await.ready(executeTransaction(actions: _*), 15.seconds)
      }
    } catch {
      // TODO: Handle this better
      case tx:TimeoutException => throw CouldNotRunDbIoActionException(tx)
      case NonFatal(x) => throw CouldNotRunDbIoActionException(x)
    }
  }
}

/**
  * Unnecessary once a config file is set up
  */
object DBUrls {
  sealed trait URL
  case class H2(url: String) extends URL
  case object H2Mem extends URL

  def connectWithUrl(u: URL) = u match {
    case H2(url) => Database.forURL(s"jdbc:h2:$url", driver = "org.h2.Driver")
    case H2Mem   => Database.forURL("jdbc:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
  }
}


/**
  * Problems schema object, defines the PROBLEMS table schema and related queries
  */
object Problems {

  /**
    * The table name
    */
  val tableName = "PROBLEMS"

  /**
    * The Problems Table. This is the table schema.
    */
  class ProblemsT(tag: Tag) extends Table[ProblemDigest](tag, tableName) {
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
    def rowToProblem(args: (String, String, String, String, String)): ProblemDigest = {
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
      * Equivalent to Select * from Problems;
      */
    val selectAll = this
    /**
      * Selects all the details xml sorted by the problem's time stamp.
      */
    val selectDetails = this.map(_.xml)
    /**
      * Selects the last 20 problems
      */
    val last20Problems = this.sortBy(_.stampText.asc).take(20)
  }

}

case class CouldNotRunDbIoActionException(exception: Throwable) extends RuntimeException(exception) {
  //TODO: Datasource
  override def getMessage:String = s"Could not use the database due to ${exception.getLocalizedMessage}"
}