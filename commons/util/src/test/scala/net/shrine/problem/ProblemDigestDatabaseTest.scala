package net.shrine.problem

import org.scalatest.FlatSpec
import slick.lifted.{TableQuery, Tag}
import slick.driver.SQLiteDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.xml.NodeSeq

/**
  * Test creation, insertion, querying, and deletion of ProblemDigest values into an
  * in-memory sqlite3 database.
  */
class ProblemDigestDatabaseTest extends FlatSpec {
  "The Database" should "Connect without any problems" in {
    val db = Database.forURL("jdbc:sqlite::memory", driver = "org.sqlite.JDBC")
    //val problems = TableQuery[Problems]
    val suppliers = TableQuery[Suppliers]
    val schema = suppliers.schema
    val setup = DBIO.seq(
      schema.create,
      suppliers += (101, "Acme, Inc.",      "99 Market Street", "Groundsville", "CA", "95199"),
      suppliers += ( 49, "Superior Coffee", "1 Party Place",    "Mendocino",    "CA", "95460"),
      suppliers += (150, "The High Ground", "100 Coffee Lane",  "Meadows",      "CA", "93966")
    )
    val setupFuture = db.run(setup)
    db.run(suppliers.result).map(_.foreach {
      case (id, name, street, city, state, zip) =>
        println("  " + id  + "\t" + name + "\t" + street + "\t" + city + "\t" + state + "\t" + zip)
    })
    db.close()
  }
}

// Definition of the PROBLEMS table
class Problems(tag: Tag) extends Table[ProblemDigest](tag, "PROBLEMS") {
  def codec = column[String]("codec")
  def stampText = column[String]("stampText")
  def summary = column[String]("summary")
  def description = column[String]("description")
  def xml = column[XML]("node seq")
  def * = (codec, stampText, summary, description, xml) <> ((ProblemDigest.apply _).tupled, ProblemDigest.unapply)

  def tupling(problem: ProblemDigest) = {
    (problem.codec, problem.stampText, problem.summary, problem.description, problem.detailsXml)
  }
}

// Definition of the SUPPLIERS table, slick example
class Suppliers(tag: Tag) extends Table[(Int, String, String, String, String, String)](tag, "SUPPLIERS") {
  def id = column[Int]("SUP_ID", O.PrimaryKey) // This is the primary key column
  def name = column[String]("SUP_NAME")
  def street = column[String]("STREET")
  def city = column[String]("CITY")
  def state = column[String]("STATE")
  def zip = column[String]("ZIP")
  // Every table needs a * projection with the same type as the table's type parameter
  def * = (id, name, street, city, state, zip)
}