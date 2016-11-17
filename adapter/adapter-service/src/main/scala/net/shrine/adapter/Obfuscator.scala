package net.shrine.adapter

import com.typesafe.config.Config
import net.shrine.log.Log
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

  //todo a problem instead?
  if((stdDev < 6.5) || (noiseClamp < 10) || (binSize < 5)) Log.warn(s"$this does not include enough obfuscation to prevent an unobservable reidentificaiton attack. We recommend stdDev >= 6.5, noiseClamp >= 10, and binSize >= 5")

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

//note that the bounds of this obfuscation are actually clamp + binSize/2
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

    if((l > noiseClamp) && (l > 10)) { //todo fix with SHRINE-1716
      //bin
      val binned = roundToNearest(l, binSize)

      //add noise
      val noised = binned + clampedGaussian(binned, noiseClamp)

      //bin again
      val rounded = roundToNearest(noised, binSize)

      //finally, if rounded is clamp or smaller, report that the result is too small.
      if ((rounded > noiseClamp) && (rounded > 10)) rounded //todo fix with SHRINE-1716
      else Obfuscator.LESS_THAN_CLAMP //will be reported as "$clamped or fewer"
    }
    else Obfuscator.LESS_THAN_CLAMP
  }
}

object Obfuscator {
  def apply(config:Config): Obfuscator = Obfuscator(config.getInt("binSize"),config.getDouble("sigma"),config.getInt("clamp"))

  val LESS_THAN_CLAMP = -1L
}