package net.shrine.adapter

import com.typesafe.config.Config
import net.shrine.protocol.QueryResult

import scala.util.Random

/**
  * @author Ricardo De Lima
  * @author Bill Simons
  * @author clint
  * @author dwalend
  * @since August 18, 2009
  * @see http://cbmi.med.harvard.edu
 */
case class Obfuscator(binSize:Int,stdDev:Double,noiseClamp:Int) {
  val random = new Random

  def obfuscateResults(doObfuscation: Boolean)(results: Seq[QueryResult]): Seq[QueryResult] = {
    if (doObfuscation) results.map(obfuscate) else results
  }

  def obfuscate(result: QueryResult): QueryResult = {
    val withObfscSetSize = result.modifySetSize(obfuscate)

    if (withObfscSetSize.breakdowns.isEmpty) {
      withObfscSetSize
    }
    else {
      val obfuscatedBreakdowns = result.breakdowns.mapValues(_.mapValues(obfuscate))

      withObfscSetSize.withBreakdowns(obfuscatedBreakdowns)
    }
  }

  def obfuscate(l: Long): Long = {

    def roundToNearest(i: Double, n: Double): Long = {
      Math.round(
        if ((i % n) >= n / 2) i + n - (i % n) //round up
        else i - i % n //round down
      )
    }

    def clampedGaussian(i: Long, clamp: Long): Double = {
      val noise = random.nextGaussian() * stdDev
      //clamp it
      if (noise > clamp) clamp
      else if (noise < -clamp) -clamp
      else noise
    }

    //bin
    val binned = roundToNearest(l, binSize)

    //add noise
    val noised = binned + clampedGaussian(binned, noiseClamp)

    //bin again
    roundToNearest(noised, binSize)
  }
}

object Obfuscator {
  def apply(config:Config): Obfuscator = Obfuscator(config.getInt("binSize"),config.getDouble("sigma"),config.getInt("clamp"))
}