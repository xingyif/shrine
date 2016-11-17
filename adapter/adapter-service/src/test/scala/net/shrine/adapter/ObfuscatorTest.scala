package net.shrine.adapter

import org.junit.Test
import net.shrine.protocol.QueryResult
import net.shrine.protocol.ResultOutputType
import net.shrine.protocol.I2b2ResultEnvelope
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.protocol.DefaultBreakdownResultOutputTypes

/**
 * @author clint
 * @since Oct 22, 2012
 */

object ObfuscatorTest {
  private def within(range: Long)(a: Long, b: Long) = scala.math.abs(b - a) <= range
  def within3 = within(3)_
}

final class ObfuscatorTest extends ShouldMatchersForJUnit {



  def assertWithin(range:Long)(a:Long, b:Long) = {
    assert(ObfuscatorTest.within(range)(a,b),s"$b was not within $range of $a")
  }

  def assertWithin3 = assertWithin(3) _
  def assertWithin1 = assertWithin(1) _


  def assertWithinOrLessThanClamp(range:Long)(a:Long, b:Long) = {
    assert(ObfuscatorTest.within(range)(a, b) || b == Obfuscator.LESS_THAN_CLAMP,
      s"$b was not within $range of $a, and $b is not ${Obfuscator.LESS_THAN_CLAMP}")
  }

  val obfuscator13 = Obfuscator(1,1.3,3)
  val obfuscator65 = Obfuscator(binSize = 5,stdDev = 6.5,noiseClamp = 10)

  @Test
  def testObfuscateLong() {
    val l = 12345L

    val obfuscated = obfuscator13.obfuscate(l)

    assertWithin3(l, obfuscated)
  }

  @Test
  def testObfuscateAlreadyObfuscated() {
    val l = Obfuscator.LESS_THAN_CLAMP

    val obfuscated = obfuscator65.obfuscate(l)

    obfuscated should be (Obfuscator.LESS_THAN_CLAMP)
  }


  @Test
  def testObfuscateZero() {
    val l = 0L

    val obfuscated = obfuscator65.obfuscate(l)

    obfuscated should be (Obfuscator.LESS_THAN_CLAMP)
  }

  @Test
  def testObfuscateVerySmall() {
    val l = 3L

    val obfuscated = obfuscator65.obfuscate(l)

    obfuscated should be (Obfuscator.LESS_THAN_CLAMP)
  }

  @Test
  def testObfuscatePrettySmall() {
    val l = 7L

    val obfuscated = obfuscator65.obfuscate(l)

    obfuscated should be (Obfuscator.LESS_THAN_CLAMP)
  }

  @Test
  def testObfuscateSmallish() {
    val l = 11L

    val obfuscated = obfuscator65.obfuscate(l)

    assertWithinOrLessThanClamp(10L)(l,obfuscated)
  }

  @Test
  def testObfuscateUseful() {
    val l = 203L

    val obfuscated = obfuscator65.obfuscate(l)

    assertWithin(15L)(l,obfuscated)
  }


  @Test
  def testObfuscateQueryResult() {
    import DefaultBreakdownResultOutputTypes._
    import ResultOutputType._

    val breakdowns = Map(
        PATIENT_AGE_COUNT_XML -> I2b2ResultEnvelope(PATIENT_AGE_COUNT_XML, Map("x" -> 1, "y" -> 42)),
    		PATIENT_GENDER_COUNT_XML -> I2b2ResultEnvelope(PATIENT_GENDER_COUNT_XML, Map("a" -> 123, "b" -> 456)))

    def queryResult(resultId: Long, setSize: Long) = QueryResult(
      resultId = resultId,
      instanceId = 123L,
      resultType = Some(PATIENT_COUNT_XML),
      setSize = setSize,
      startDate = None,
      endDate = None,
      description = None,
      statusType = QueryResult.StatusType.Finished,
      statusMessage = None,
      breakdowns = breakdowns
    )

    val resultId1 = 12345L

    val setSize1 = 123L

    //No breakdowns
    {
      val noBreakdowns = queryResult(resultId1, setSize1).copy(breakdowns = Map.empty)

      noBreakdowns.setSize should equal(setSize1)
      noBreakdowns.breakdowns should equal(Map.empty)

      val obfuscated = obfuscator13.obfuscate(noBreakdowns)

      assertWithin3(noBreakdowns.setSize, obfuscated.setSize)
      obfuscated.breakdowns should equal(Map.empty)
    }

    //breakdowns
    {
      val QueryResult(_, _, _, obfscSetSize1, _, _, _, _, _, _, obfscBreakdowns) = obfuscator13.obfuscate(queryResult(resultId1, setSize1))

      assertWithin3(setSize1, obfscSetSize1)
      
      breakdowns.keySet should equal(obfscBreakdowns.keySet)
      
      for {
        (resultType, obfscEnv) <- obfscBreakdowns
        env <- breakdowns.get(resultType) 
      } {
        env.data.keySet should equal(obfscEnv.data.keySet)
        
        for {
          (key, value) <- env.data
          obfscValue <- obfscEnv.data.get(key)
        } {
          assertWithin3(value, obfscValue)
        }
      }
    }
  }
}