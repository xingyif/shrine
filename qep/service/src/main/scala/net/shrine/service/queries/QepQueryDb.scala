package net.shrine.service.queries

import java.io.PrintWriter
import java.sql.{DriverManager, Connection, SQLException}
import java.util.logging.Logger
import javax.naming.InitialContext
import javax.sql.DataSource

import com.typesafe.config.Config
import net.shrine.log.Loggable
import net.shrine.protocol.RunQueryRequest
import net.shrine.audit.{Time, QueryName, NetworkQueryId, UserName}
import net.shrine.service.QepConfigSource

import slick.driver.JdbcProfile
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.{Duration,DurationInt}
import scala.language.postfixOps

import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.blocking

/**
  * DB code for the QEP's query instances and query results.
  *
  * @author david
  * @since 1/19/16
  */
case class QepQueryDb(schemaDef:QepQuerySchema,dataSource: DataSource) extends Loggable {
  import schemaDef._
  import jdbcProfile.api._

  val database = Database.forDataSource(dataSource)

  def createTables() = schemaDef.createTables(database)

  def dropTables() = schemaDef.dropTables(database)

  def dbRun[R](action: DBIOAction[R, NoStream, Nothing]):R = {
    val future: Future[R] = database.run(action)
    blocking {
      Await.result(future, 10 seconds)
    }
  }

  def insertQepQuery(runQueryRequest: RunQueryRequest):Unit = {
    debug(s"insertQepQuery $runQueryRequest")

    insertQepQuery(QepQuery(runQueryRequest))
  }

  def insertQepQuery(qepQuery: QepQuery):Unit = {
    dbRun(allQepQueryQuery += qepQuery)
  }

  def selectAllQepQueries:Seq[QepQuery] = {
    dbRun(allQepQueryQuery.result)
  }

}

object QepQueryDb extends Loggable {

  val dataSource:DataSource = {

    val dataSourceFrom = QepQuerySchema.config.getString("dataSourceFrom")
    if(dataSourceFrom == "JNDI") {
      val jndiDataSourceName = QepQuerySchema.config.getString("jndiDataSourceName")
      val initialContext:InitialContext = new InitialContext()

      initialContext.lookup(jndiDataSourceName).asInstanceOf[DataSource]

    }
    else if (dataSourceFrom == "testDataSource") {

      val testDataSourceConfig = QepQuerySchema.config.getConfig("testDataSource")
      val driverClassName = testDataSourceConfig.getString("driverClassName")
      val url = testDataSourceConfig.getString("url")

      //Creating an instance of the driver register it. (!) From a previous epoch, but it works.
      Class.forName(driverClassName).newInstance()

      object TestDataSource extends DataSource {
        override def getConnection: Connection = {
          DriverManager.getConnection(url)
        }

        override def getConnection(username: String, password: String): Connection = {
          DriverManager.getConnection(url, username, password)
        }

        //unused methods
        override def unwrap[T](iface: Class[T]): T = ???
        override def isWrapperFor(iface: Class[_]): Boolean = ???
        override def setLogWriter(out: PrintWriter): Unit = ???
        override def getLoginTimeout: Int = ???
        override def setLoginTimeout(seconds: Int): Unit = ???
        override def getParentLogger: Logger = ???
        override def getLogWriter: PrintWriter = ???
      }

      TestDataSource
    }
    else throw new IllegalArgumentException(s"shrine.steward.database.dataSourceFrom must be either JNDI or testDataSource, not $dataSourceFrom")
  }

  val db = QepQueryDb(QepQuerySchema.schema,dataSource)

  val createTablesOnStart = QepQuerySchema.config.getBoolean("createTablesOnStart")
  if(createTablesOnStart) QepQueryDb.db.createTables()

}

/**
  * Separate class to support schema generation without actually connecting to the database.
  *
  * @param jdbcProfile Database profile to use for the schema
  */
case class QepQuerySchema(jdbcProfile: JdbcProfile) extends Loggable {
  import jdbcProfile.api._

  def ddlForAllTables = {
    allQepQueryQuery.schema
  }

  //to get the schema, use the REPL
  //println(QepQuerySchema.schema.ddlForAllTables.createStatements.mkString(";\n"))

  def createTables(database:Database) = {
    try {
      val future = database.run(ddlForAllTables.create)
      Await.result(future,10 seconds)
    } catch {
      //I'd prefer to check and create schema only if absent. No way to do that with Oracle.
      case x:SQLException => info("Caught exception while creating tables. Recover by assuming the tables already exist.",x)
    }
  }

  def dropTables(database:Database) = {
    val future = database.run(ddlForAllTables.drop)
    //Really wait forever for the cleanup
    Await.result(future,Duration.Inf)
  }

  /**
  mysql> describe SHRINE_QUERY;
+------------------+--------------+------+-----+-------------------+----------------+
| Field            | Type         | Null | Key | Default           | Extra          |
+------------------+--------------+------+-----+-------------------+----------------+
| id               | int(11)      | NO   | PRI | NULL              | auto_increment |
| local_id         | varchar(255) | NO   | MUL | NULL              |                |
| network_id       | bigint(20)   | NO   | MUL | NULL              |                |
| username         | varchar(255) | NO   | MUL | NULL              |                |
| domain           | varchar(255) | NO   |     | NULL              |                |
| query_name       | varchar(255) | NO   |     | NULL              |                |
| query_expression | text         | YES  |     | NULL              |                |
| date_created     | timestamp    | NO   |     | CURRENT_TIMESTAMP |                |
| has_been_run     | tinyint(1)   | NO   |     | 0                 |                |
| flagged          | tinyint(1)   | NO   |     | 0                 |                |
| flag_message     | varchar(255) | YES  |     | NULL              |                |
| query_xml        | text         | YES  |     | NULL              |                |
+------------------+--------------+------+-----+-------------------+----------------+
    */

  class QepQueries(tag:Tag) extends Table[QepQuery](tag,"queries") {
    def networkId = column[NetworkQueryId]("networkId")
    def userName = column[UserName]("userName")
    def userDomain = column[String]("domain")
    def queryName = column[QueryName]("queryName")
    def expression = column[String]("expression")
    def dateCreated = column[Time]("dateCreated")
    def hasBeenRun = column[Boolean]("hasBeenRun")
    def flagged = column[Boolean]("flagged")
    def flagMessage = column[String]("flagMessage")
    def queryXml = column[String]("queryXml")


    def * = (networkId,userName,userDomain,queryName,expression,dateCreated,hasBeenRun,flagged,flagMessage,queryXml) <> (QepQuery.tupled,QepQuery.unapply)

  }
  val allQepQueryQuery = TableQuery[QepQueries]
}

object QepQuerySchema {

  val allConfig:Config = QepConfigSource.config
  val config:Config = allConfig.getConfig("shrine.queryEntryPoint.queries.database")

  val slickProfileClassName = config.getString("slickProfileClassName")
  val slickProfile:JdbcProfile = QepConfigSource.objectForName(slickProfileClassName)

  val schema = QepQuerySchema(slickProfile)
}

case class QepQuery(
                     networkId:NetworkQueryId,
                     userName: UserName,
                     userDomain: String,
                     queryName: QueryName,
                     expression: String,
                     dateCreated: Time,
                     hasBeenRun: Boolean,
                     flagged: Boolean,
                     flagMessage: String,
                     queryXml:String
                   )

object QepQuery extends ((NetworkQueryId,UserName,String,QueryName,String,Time,Boolean,Boolean,String,String) => QepQuery) {
  def apply(runQueryRequest: RunQueryRequest):QepQuery = {
    new QepQuery(
      runQueryRequest.networkQueryId,
      runQueryRequest.authn.username,
      runQueryRequest.authn.domain,
      runQueryRequest.queryDefinition.name,
      runQueryRequest.queryDefinition.expr.getOrElse("No Expression").toString,
      System.currentTimeMillis(),
      false,
      false, //todo flagged??
      "", //todo flagMessage
      runQueryRequest.toXmlString
    )
  }
}
