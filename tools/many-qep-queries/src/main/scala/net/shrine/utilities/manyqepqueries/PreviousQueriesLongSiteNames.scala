package net.shrine.utilities.manyqepqueries

import net.shrine.qep.querydb.QepQueryDb

import scala.util.{Failure, Random, Success, Try}

/**
  * @author lbakker
  */

object PreviousQueriesLongSiteNames {
  /**
    * This is the runner method which calls a higher order function 'utilities.setupNetwork(args)' which will run
    * the network setup within the utilities class using the given block of code.
    *
    * @param args the parameters for the network size and amount of previous queries.
    * @return The finished network.
    */
  def main(args: Array[String]): Int = {
    Utilities.setupNetwork(args) {
      val adapterCount = Integer.parseInt(args(0))
      val queryCount = Integer.parseInt(args(1))
      val adapterNames = setNetworkSizeLongNames(adapterCount)
      val resultsToInsert = Utilities.setNumQueries(queryCount, adapterNames)
      resultsToInsert.foreach(nToR => QepQueryDb.db.insertQueryResult(nToR._1, nToR._2))
    }
  }

  /**
    * Sets the network to be a certain size depending the param 'adapter count'
    *
    * @param adapterCount the intended size of the network
    * @return the indexedSeq of sites
    */
  def setNetworkSizeLongNames(adapterCount: Int): Seq[String] = {
    val random = new Random

    def randomCapital = (Math.abs(random.nextInt() % 26) + 65).toChar

    return (1 to adapterCount).map(adapterNumber => s"${randomCapital}${randomCapital}" +
      s"${randomCapital}${randomCapital}${randomCapital}${randomCapital}${randomCapital}" +
      s"${randomCapital}${randomCapital}${randomCapital}${randomCapital}${randomCapital}${randomCapital}")
  }


}




