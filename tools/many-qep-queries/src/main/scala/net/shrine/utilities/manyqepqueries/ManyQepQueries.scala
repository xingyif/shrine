package net.shrine.utilities.manyqepqueries

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import net.shrine.protocol.QueryResult
import net.shrine.qep.queries.{QepQuery, QepQueryDb}
import net.shrine.source.ConfigSource

import scala.util.Random

/**
 * @author dwalend
 * @since 1.23.3.1
 */

object ManyQepQueries {
  def main(args: Array[String]): Unit = {
    try {
      if (args.length != 4) throw new WrongNumberOfArguments("Requires four arguments: The number of adapters, the number of queries per adapter , the full path to the many-qep-queries.conf file, and the full path to the shrine.conf file.")

      val adapterCount = Integer.parseInt(args(0))
      val queryCount = Integer.parseInt(args(1))
      val localConfig = args(2)
      val shrineConfig = args(3)

      val config: Config = ConfigFactory.parseFile(new File(localConfig)).withFallback(ConfigFactory.parseFile(new File(shrineConfig))).withFallback(ConfigFactory.load())

      ConfigSource.configForBlock(config, getClass.getSimpleName) {

        val random = new Random
        val currentTime =  System.currentTimeMillis()
        val queriesToInsert = (1 to queryCount).map(queryNumber => new QepQuery(
          networkId = random.nextLong(),
          userName = "shrine",
          userDomain = "i2b2demo",
          queryName = s"Lungs $queryNumber",
          expression = Some("Fake Query Expression"),
          dateCreated = currentTime - 2000000 + queryNumber * 10000,
          deleted = false,
          queryXml = "Fake Query XML",
          changeDate = currentTime - 2000000 + queryNumber * 10000
        ))

        queriesToInsert.foreach(QepQueryDb.db.insertQepQuery)

        def randomCapital = (Math.abs(random.nextInt() % 26 ) + 65).toChar
        val adapterNames =  (1 to adapterCount).map(adapterNumber => s"${randomCapital}${randomCapital}${randomCapital}${randomCapital}${randomCapital}${randomCapital}")

        val resultsToInsert = adapterNames.flatMap{ adapterName =>
          queriesToInsert.map { query =>
            val queryResult = QueryResult(
              resultId = random.nextLong(),
              instanceId = random.nextLong(),
              resultType = None,
              setSize = (((Math.abs(random.nextInt()) % 20000) / 5 ) * 5) + 10,
              startDate = None,
              endDate = None,
              description = Some(adapterName),
              statusType = QueryResult.StatusType.Finished,
              statusMessage = None
            )
            (query.networkId,queryResult)
          }
        }

        resultsToInsert.foreach(nToR => QepQueryDb.db.insertQueryResult(nToR._1,nToR._2))
      }

      println("previousQueries and queryResults injected into the QEP cache.")
    }
    catch {
      case wnoa:WrongNumberOfArguments =>
        printUsage()
        println(wnoa.message)
        System.exit(1)
      case x: Throwable =>
        printUsage()
        x.printStackTrace()
        System.exit(2)
    }

    def printUsage(): Unit = {
      println(
        """
          |Usage: ./many-qep-queries NUMBER_OF_ADAPTERS NUMBER_OF_QUERIES ADAPTER_CONF SHRINE_CONF
          |Inject previous queries and results into the local qepAuditDB database.
          |
          |ADAPTER_CONF is a configuration file for this tool. This conf file takes precedence over the SHRINE_CONF file.
          |  See conf/adapter-queries-to-qep.conf for an example.
          |SHRINE_CONF is the local configuration file, usually /opt/shrine/tomcat/lib/shrine.conf .
          |
          |Exit codes: 0 success
          |            1 known error
          |            2 unknown error (with an accompanying stack trace).
          |
          |Example: ./many-qep-queries 63 40 conf/adapter-queries-to-qep.conf /opt/shrine/tomcat/lib/shrine.conf
        """.stripMargin)
    }

    case class WrongNumberOfArguments(message:String) extends Exception(message)
  }
}