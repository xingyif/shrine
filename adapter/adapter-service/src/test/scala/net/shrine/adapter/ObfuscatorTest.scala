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
  
  def within3 = within(3) _
  def within1 = within(1) _
}

final class ObfuscatorTest extends ShouldMatchersForJUnit {
  import ObfuscatorTest._

  val obfuscator = Obfuscator(1,1.3,3)

  @Test
  def testObfuscateLong() {
    val l = 12345L
    
    val obfuscated = obfuscator.obfuscate(l)
    
    within3(l, obfuscated) should be(right = true)
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
      
      val obfuscated = obfuscator.obfuscate(noBreakdowns)
      
      within3(noBreakdowns.setSize, obfuscated.setSize)
      obfuscated.breakdowns should equal(Map.empty)
    }
    
    //breakdowns
    {
      val QueryResult(_, _, _, obfscSetSize1, _, _, _, _, _, _, obfscBreakdowns) = obfuscator.obfuscate(queryResult(resultId1, setSize1))
              
      within3(setSize1, obfscSetSize1) should be(right = true)
      
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
          within3(value, obfscValue) should be(right = true)
        }
      }
    }
  }
}