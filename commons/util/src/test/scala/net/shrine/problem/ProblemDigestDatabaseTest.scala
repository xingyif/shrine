package net.shrine.problem

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import slick.lifted.{TableQuery, Tag}
import slick.driver.SQLiteDriver.api._ // Fix this wildcard import
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.xml.XML

/**
  * Test creation, insertion, querying, and deletion of ProblemDigest values into an
  * in-memory sqlite3 database. Demonstrates proof of concept mapping of ProblemDigest
  * case class into a database.
  */
class ProblemDigestDatabaseTest extends FlatSpec with BeforeAndAfter with ScalaFutures with Matchers {
  implicit val defaultPatience =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(10, Millis))
  val db = Database.forURL("jdbc:sqlite:memory", driver = "org.sqlite.JDBC")
  val suppliers = Suppliers.Queries.suppliers
  val problems = Problems.Queries.problems
  val schema = suppliers.schema ++ problems.schema
  val problemDigests = Seq(
    // Not actually sure what examples of ProblemDigests look like
    ProblemDigest("MJPG", "01:01:01", "summary here", "description here", <details>uh not sure</details>),
    ProblemDigest("codec", "01:01:02", "such summary", "such description", <details>more details</details>))
  val supplierTuples = Seq(
    (101, "Acme, Inc."     , "99 Market Street", "San Luis" , "CA", "95199"),
    (49 , "Superior Coffee", "1 Party Place"   , "Mendocino", "CA", "95460"),
    (200, "Pavement Coffee", "50 Leon St"      , "Boston"   , "MA", "02115"))

  before {
    // Create the tables in the database
    Await.ready(db.run(schema.create), Duration.Inf)

    // Ensure that the table intially has nothing in it
    whenReady (db.run(problems.result)) { result =>
      result shouldBe empty
    }
    whenReady (db.run(suppliers.result)) { result =>
      result shouldBe empty
    }
  }

  after {
    Await.ready(db.run(schema.drop), Duration.Inf)
    // After dropping the tables, there should be nothing left, thus this throws a
    // test failed exception. Technically the exception is due to a java sql exception.
    // Should it be refactored?
    // TODO: Refactor to show sql exception
    intercept[org.scalatest.exceptions.TestFailedException] {
      whenReady(db.run(problems.result)) { result =>
        result shouldBe empty
      }
    }
    db.close()
  }

  "The Database" should "Connect without any problems" in {
    // Insert the suppliers and ProblemDigests
    Await.ready(db.run(DBIO.seq(problems ++= problemDigests, suppliers ++= supplierTuples)), Duration.Inf)

    // Test that they are all in the table
    whenReady (db.run(suppliers.result)) { result =>
      result should contain theSameElementsAs supplierTuples
      result should have length supplierTuples.length
    }
    whenReady (db.run(problems.result)) { result =>
      result should contain theSameElementsAs problemDigests
      result should have length problemDigests.length
    }

    // Test that the simple select and filter queries work
    whenReady (db.run(Suppliers.Queries.filterBoston.result)) { result =>
      result.head shouldBe "Pavement Coffee"
      result should have length 1
    }
    // This also tests that our conversion from xml to strings works
    whenReady (db.run(Problems.Queries.selectDetails.result)) { result =>
      result should have length problemDigests.length
      result should contain theSameElementsAs problemDigests.map(_.detailsXml.toString())
    }

    // Tests that show deleting queries work.
    whenReady(db.run(suppliers.delete)) { result =>
      // 3 items are deleted
      result shouldBe supplierTuples.length
    }
    whenReady(db.run(problems.delete)) { result =>
      result shouldBe problemDigests.length
    }
    // And when we try to select, the result list is empty
    whenReady(db.run(problems.result)) { result =>
      result shouldBe empty
    }
  }
}

/**
  * Problems schema object, defines the PROBLEMS table schema and related queries
  */
object Problems {

  // Definition of the PROBLEMS table
  class ProblemsT(tag: Tag) extends Table[ProblemDigest](tag, "PROBLEMS") {
    def codec = column[String]("codec")

    def stampText = column[String]("stampText", O.PrimaryKey)

    def summary = column[String]("summary")

    def description = column[String]("description")

    def xml = column[String]("detailsXml")

    // Monad Type Hackery
    def * = (codec, stampText, summary, description, xml) <> (rowToProblem, problemToRow)

    // Converts a table row into a ProblemDigest
    // I feel like this is somehow flipped with untupled, you can always convert
    // a ProblemDigest to a row, but converting a row to a ProblemDigest can sometimes fail
    def rowToProblem(args: (String, String, String, String, String)): ProblemDigest = args match {
      case (codec, stampText, summary, description, detailsXml) =>
        ProblemDigest(codec, stampText, summary, description, XML.loadString(detailsXml))
    }

    // Converts a ProblemDigest into an Option of a table row
    // TODO: Figure out if there are constraints on strings stored in databases that I need to check here,
    // TODO: For instance will string length be an issue?
    def problemToRow(problem: ProblemDigest): Option[(String, String, String, String, String)] = {
        Some((problem.codec, problem.stampText, problem.summary, problem.description, problem.detailsXml.toString))
    }
  }

  object Queries {
    // Select *
    val problems = TableQuery[ProblemsT]
    // Selects the detailXml value sorted by their timeStamp
    val selectDetails = problems.sortBy(_.stampText.asc).map(_.xml)
  }
}

/**
  *  Suppliers schema object, holds the SUPPLIERS table schema and pre-defined queries for it
  */
object Suppliers {

  // Definition of the SUPPLIERS table, slick example
  class SuppliersT(tag: Tag) extends Table[(Int, String, String, String, String, String)](tag, "SUPPLIERS") {
    def id = column[Int]("SUP_ID", O.PrimaryKey)

    // This is the primary key column
    def name = column[String]("SUP_NAME")

    def street = column[String]("STREET")

    def city = column[String]("CITY")

    def state = column[String]("STATE")

    def zip = column[String]("ZIP")

    // Every table needs a * projection with the same type as the table's type parameter
    def * = (id, name, street, city, state, zip)
  }


  object Queries {
    // Select *
    val suppliers = TableQuery[SuppliersT]
    // Simple filter
    val filterBoston = suppliers.filter(_.city === "Boston").map(_.name)
  }

}