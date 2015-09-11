package net.shrine.authentication

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @since Jul 1, 2014
 */
final class AuthenticationTypeTest extends ShouldMatchersForJUnit {
  @Test
  def testNames: Unit = {
    import AuthenticationType._

    Ecommons.name should equal("ecommons")
    Pm.name should equal("pm")
    NoAuthentication.name should equal("none")
  }
  
  @Test
  def testValueOf: Unit = {
    import AuthenticationType._
    
    valueOf("") should be(None)
    valueOf(null) should be(None)
    valueOf("asdkjhasdj") should be(None)
    
    valueOf("ecommons").get should be(Ecommons)
    valueOf("Ecommons").get should be(Ecommons)
    valueOf("ECOMMONS").get should be(Ecommons)
    valueOf("eCoMmOnS").get should be(Ecommons)
    
    valueOf("pm").get should be(Pm)
    valueOf("Pm").get should be(Pm)
    valueOf("PM").get should be(Pm)
    valueOf("pM").get should be(Pm)
    
    valueOf("none").get should be(NoAuthentication)
    valueOf("None").get should be(NoAuthentication)
    valueOf("NONE").get should be(NoAuthentication)
    valueOf("nOnE").get should be(NoAuthentication)
  }
}