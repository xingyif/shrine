package net.shrine.adapter

import net.shrine.protocol.QueryResult
import java.util.Random

/**
 * @author Bill Simons
 * @author clint
 * @date 4/21/11
 * @link http://cbmi.med.harvard.edu
 */
object Obfuscator {
  def obfuscate(result: QueryResult): QueryResult = {
    val withObfscSetSize = result.modifySetSize(obfuscate)
    
    if(withObfscSetSize.breakdowns.isEmpty) { withObfscSetSize }
    else {
      val obfuscatedBreakdowns = result.breakdowns.mapValues(_.mapValues(obfuscate))
      
      withObfscSetSize.withBreakdowns(obfuscatedBreakdowns)
    }
  }

  def obfuscate(l: Long): Long = {
    import GaussianObfuscator._

    val obfuscationAmount = determineObfuscationAmount(l)

    determineObfuscatedSetSize(l, obfuscationAmount)
  }
  
  def obfuscateResults(doObfuscation: Boolean)(results: Seq[QueryResult]): Seq[QueryResult] = {
    if(doObfuscation) results.map(obfuscate) else results
  }

  /**
   * @author clint
   * @author Ricardo De Lima 
   * @date August 18, 2009
   * 
   * Harvard Medical School Center for BioMedical Informatics
   *
   * @link http://cbmi.med.harvard.edu
   */
  object GaussianObfuscator {
    private val stdDev = 1.33

    private val mean = 0

    private val rand = new Random

    val range = 3

    private val lower = (-range).toDouble

    private val upper = range.toDouble

    def determineObfuscationAmount(x: Long): Int = scala.math.round(gaussian(mean, stdDev)).toInt

    def determineObfuscatedSetSize(setSize: Long, obfuscationAmount: Int): Long = {
      if (setSize <= 10) { -1L }
      else { setSize + obfuscationAmount }
    }

    /**
     * Return a real number from a gaussian distribution with given mean and
     * stddev
     */
    def gaussian(mean: Double, stddev: Double): Double = {
      def limitRange(v: Double): Double = {
        val partiallyClamped = if (v < lower) lower else v

        if (partiallyClamped > upper) upper else partiallyClamped
      }

      limitRange(mean + (stddev * rand.nextGaussian))
    }
  }
}