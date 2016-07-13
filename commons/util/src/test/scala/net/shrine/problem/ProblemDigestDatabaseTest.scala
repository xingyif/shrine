package net.shrine.problem

import org.scalatest.FlatSpec
import slick.lifted.{TableQuery, Tag}
import slick.driver.SQLiteDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.XML

/**
  * Test creation, insertion, querying, and deletion of ProblemDigest values into an
  * in-memory sqlite3 database.
  */
class ProblemDigestDatabaseTest extends FlatSpec {
  "The Database" should "Connect without any problems" in {
    val db = Database.forURL("jdbc:sqlite::memory", driver = "org.sqlite.JDBC")
    val problems = Problems.problems
    val suppliers = Suppliers.suppliers
    val schema = problems.schema.create
    val setup = DBIO.seq(
      schema,
      suppliers += (101, "Acme, Inc.", "99 Market Street", "Groundsville", "CA", "95199"),
      suppliers += (49, "Superior Coffee", "1 Party Place", "Mendocino", "CA", "95460"),
      suppliers += (150, "The High Ground", "100 Coffee Lane", "Meadows", "CA", "93966"),
      suppliers += (200, "Pavement", "50 Leon St", "Boston", "MA", "02115"),
      // Not actually sure what examples of ProblemDigests look like
      problems += ProblemDigest("MJPG", "01:01:01", "summary here", "description here", <details>uh not sure</details>),
      problems += ProblemDigest("codec", "01:01:02", "such summary", "such description", <details>more details</details>),\
    )
    db.run(setup).onSuccess(db => println("success"))
    db.close()
  }
}

object Problems {

  // Definition of the PROBLEMS table
  class ProblemsT(tag: Tag) extends Table[ProblemDigest](tag, "PROBLEMS") {
    def codec = column[String]("codec")

    def stampText = column[String]("stampText", O.PrimaryKey)

    def summary = column[String]("summary")

    def description = column[String]("description")

    def xml = column[String]("detailsXml")

    def * = (codec, stampText, summary, description, xml) <> (tupled, untupled)

    // Converts a table row into a ProblemDigest
    // I feel like this is somehow flipped with untupled, you can always convert
    // a ProblemDigest to a row, but converting a row to a ProblemDigest can sometimes fail
    def tupled(args: (String, String, String, String, String)) = args match {
      case (codec, stampText, summary, description, detailsXml) =>
        ProblemDigest(codec, stampText, summary, description, XML.loadString(detailsXml))
    }

    // Converts a ProblemDigest into an Option of a table row
    def untupled(problem: ProblemDigest) = {
      Some((problem.codec, problem.stampText, problem.summary, problem.description, problem.detailsXml.toString()))
    }
  }

  val problems = TableQuery[ProblemsT]

  object Queries {
    // Selects the detailXml value sorted by their timeStamp
    val selectDetails = problems.sortBy(_.stampText.desc).map(_.xml)
  }
}

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

  val suppliers = TableQuery[SuppliersT]

  object Queries {
    // Simple filter
    val filterBoston = suppliers.filter(_.city === "Boston")
  }

}