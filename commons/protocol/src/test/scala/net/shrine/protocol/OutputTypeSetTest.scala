package net.shrine.protocol

import junit.framework.TestCase
import org.scalatest.junit.AssertionsForJUnit
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 *
 * @author Clint Gilbert
 * @date Sep 20, 2011
 *
 * @link http://cbmi.med.harvard.edu
 *
 * This software is licensed under the LGPL
 * @link http://www.gnu.org/licenses/lgpl.html
 *
 */
final class OutputTypeSetTest extends ShouldMatchersForJUnit {

  private val possibleOutputTypeSets = Seq(ResultOutputType.values.toSet,
    Set(ResultOutputType.PATIENT_COUNT_XML),
    Set(ResultOutputType.PATIENTSET),
    Set.empty[ResultOutputType])

  @Test
  def testConstructorAndToSet {
    possibleOutputTypeSets.foreach { outputTypes =>

      val outputTypeSet = new OutputTypeSet(outputTypes)

      outputTypeSet.toSet should equal(outputTypes)
    }

    intercept[IllegalArgumentException] {
      val nullSet: Set[ResultOutputType] = null

      new OutputTypeSet(nullSet)
    }
  }

  @Test
  def testStringConstructorAndSerialized {
    possibleOutputTypeSets.foreach { outputTypes =>
      val outputTypeSet = new OutputTypeSet(outputTypes)

      val serialized = outputTypeSet.serialized

      serialized should not be (null)

      val roundTripped = new OutputTypeSet(serialized)

      roundTripped should equal(outputTypeSet)
    }

    intercept[Exception] {
      val nullString: String = null

      new OutputTypeSet(nullString)
    }

    intercept[Exception] {
      new OutputTypeSet("jkasdhkjashdjks")
    }
  }

  @Test
  def testSerialized {
    import ResultOutputType._

    new OutputTypeSet(Set.empty[ResultOutputType]).serialized should equal("")

    new OutputTypeSet(Set(PATIENT_COUNT_XML, PATIENTSET)).serialized should equal("PATIENT_COUNT_XML%2CPATIENTSET")

    ResultOutputType.values.foreach { outputType =>
      new OutputTypeSet(Set(outputType)).serialized should equal(outputType.name)
    }
  }

  @Test
  def testDeserialize {
    import OutputTypeSet.deserialize

    intercept[Exception] {
      deserialize(null)
    }

    deserialize("") should equal(Set.empty[ResultOutputType])

    ResultOutputType.values.foreach { outputType =>
      deserialize(outputType.name) should equal(Set(outputType))
    }

    val someOutputTypes = Set(ResultOutputType.PATIENTSET, ResultOutputType.PATIENT_COUNT_XML)

    deserialize("PATIENT_COUNT_XML%2CPATIENTSET") should equal(someOutputTypes)
    deserialize("PATIENTSET%2CPATIENT_COUNT_XML") should equal(someOutputTypes)
  }
  
  @Test
  def testDeserializeAllPermutations {
    import OutputTypeSet.{deserialize, encode}
    
    val allResultOutputTypes = ResultOutputType.values.toSet
    
    ResultOutputType.values.permutations.foreach { resultTypes =>
      val serialized = encode(resultTypes.map(_.name).mkString(","))
      
      deserialize(serialized) should equal(allResultOutputTypes)
    }
  }
}