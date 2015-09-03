package net.shrine.authorization

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @since Jul 1, 2014
 */
final class AuthorizationTypeTest extends ShouldMatchersForJUnit {
  import AuthorizationType._
  
  @Test
  def testNames: Unit = {
    HmsSteward.name should equal("hms-steward")
    NoAuthorization.name should equal("none")
  }
  
  @Test
  def testValueOf: Unit = {
    valueOf("") should be(None)
    valueOf(null) should be(None)
    valueOf("asdkjhasdj") should be(None)
    
    valueOf("hms-steward").get should be(HmsSteward)
    valueOf("Hms-Steward").get should be(HmsSteward)
    valueOf("HMS-STEWARD").get should be(HmsSteward)
    valueOf("hMS-sTeWaRd").get should be(HmsSteward)
    
    valueOf("hmssteward").get should be(HmsSteward)
    valueOf("HmsSteward").get should be(HmsSteward)
    valueOf("HMSSTEWARD").get should be(HmsSteward)
    valueOf("hMssTeWaRd").get should be(HmsSteward)
    
    valueOf("none").get should be(NoAuthorization)
    valueOf("None").get should be(NoAuthorization)
    valueOf("NONE").get should be(NoAuthorization)
    valueOf("nOnE").get should be(NoAuthorization)
  }
}