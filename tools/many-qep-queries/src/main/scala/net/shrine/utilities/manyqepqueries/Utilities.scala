package net.shrine.utilities.manyqepqueries

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import net.shrine.audit.NetworkQueryId
import net.shrine.protocol.QueryResult
import net.shrine.qep.querydb.{QepQuery, QepQueryDb}
import net.shrine.source.ConfigSource

import scala.util.{Failure, Random, Success, Try}

/**
  * @author lbakker
  */

object Utilities {
  /**
    * Sets the network to be a certain size depending the param 'adapter count'
    *
    * @param adapterCount the intended size of the network
    * @return the indexedSeq of sites
    */
  def setNetworkSize(adapterCount: Int): Seq[String] = {
    val random = new Random

    def randomCapital = (Math.abs(random.nextInt() % 26) + 65).toChar

    return (1 to adapterCount).map(adapterNumber => s"${randomCapital}${randomCapital}${randomCapital}" +
      s"${randomCapital}${randomCapital}${randomCapital}")
  }


  /**
    * Sets the number of previous queries
    *
    * @param queryCount the number of previous queries
    * @return the set of queries
    */
  def setNumQueries(queryCount: Int, adapterNames: Seq[String]): Seq[(NetworkQueryId, QueryResult)] = {
    val random = new Random
    val currentTime = System.currentTimeMillis()
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

    val resultsToInsert = adapterNames.flatMap { adapterName =>
      queriesToInsert.map { query =>
        val queryResult = QueryResult(
          resultId = random.nextLong(),
          instanceId = random.nextLong(),
          resultType = None,
          setSize = (((Math.abs(random.nextInt()) % 20000) / 5) * 5) + 10,
          startDate = None,
          endDate = None,
          description = Some(adapterName),
          statusType = QueryResult.StatusType.Finished,
          statusMessage = None
        )
        (query.networkId, queryResult)
      }
    }

    resultsToInsert.foreach(nToR => QepQueryDb.db.insertQueryResult(nToR._1, nToR._2))
    queriesToInsert.foreach(QepQueryDb.db.insertQepQuery)
    println("previousQueries and queryResults injected into the QEP cache.")

    return resultsToInsert
  }

  /**
    * This method takes in the array of strings(args) and a block that returns type T
    * This call works for all different network setups because the customization comes in the 'block' section
    *
    * @param args  the setup parameters for the network
    * @param block the piece that is run to set the network to a certain test case specification
    * @tparam T
    * @return Either returns the setup for the network or gives an error message
    */
  def setupNetwork[T](args: Array[String])(block: => T): Int = {
    // for new func
    val t = Try {
      if (args.length != 4) throw new WrongNumberOfArguments(
        """Requires four arguments: The number of adapters, the number of queries per adapter ,
          | the full path to the many-qep-queries.sh.conf file, and the full path to the shrine.conf file.""".stripMargin)
      val localConfig = args(2)
      val shrineConfig = args(3)
      val config: Config = ConfigFactory.parseFile(new File(localConfig)).
        withFallback(ConfigFactory.parseFile(new File(shrineConfig))).withFallback(ConfigFactory.load())

      ConfigSource.configForBlock(config, getClass.getSimpleName) {
        block
      }
    }
    t match {
      case Success(s) => 0
      case Failure(x) => Utilities.handleFailure(x)
    }
  }

  def handleFailure(error: Throwable): Int = {
    error match {
      case wnoa: WrongNumberOfArguments =>
        printUsage()
        println(wnoa.message)
        1
      case x: Throwable =>
        printUsage()
        x.printStackTrace()
        2
    }
  }

  case class
  WrongNumberOfArguments(message: String) extends Exception(message)

  def printUsage(): Unit = {
    println(
      """
        |Usage: ./many-qep-queries.sh NUMBER_OF_ADAPTERS NUMBER_OF_QUERIES ADAPTER_CONF SHRINE_CONF
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
        |Example: ./many-qep-queries.sh 63 40 conf/adapter-queries-to-qep.conf /opt/shrine/tomcat/lib/shrine.conf
      """.stripMargin)
  }
}
