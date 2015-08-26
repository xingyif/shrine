package net.shrine.protocol

import com.typesafe.config.ConfigFactory
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @since Dec 5, 2013
 */
final class CredentialConfigTest extends ShouldMatchersForJUnit {
  @Test
  def testApply() {
    intercept[Exception] {
      CredentialConfig(ConfigFactory.empty)
    }

    {
      val config = CredentialConfig(ConfigFactory.parseString("""foo { fooCredentials {
            domain = "d"
            username = "u"
            password = "p"
          }}""").getConfig("foo.fooCredentials"))

      config.domain.get should be("d")
      config.username should be("u")
      config.password should be("p")
    }

    //No domain
    {
      val config = CredentialConfig(ConfigFactory.parseString("""foo { fooCredentials {
            username = "u"
            password = "p"
          }}""").getConfig("foo.fooCredentials"))

      config.domain should be(None)
      config.username should be("u")
      config.password should be("p")
    }

    //Bogus values
    {
      intercept[Exception] {
        CredentialConfig(ConfigFactory.parseString("""
            domain = "d"
          }}"""))
      }
      
      intercept[Exception] {
        CredentialConfig(ConfigFactory.parseString("""
            domain = "d"
            username = "u"
          }}"""))
      }
      
      intercept[Exception] {
        CredentialConfig(ConfigFactory.parseString("""
            domain = "d"
            password = "p"
          }}"""))
      }
      
      intercept[Exception] {
        CredentialConfig(ConfigFactory.parseString("""
            domain = 12345
            username = "u"
            password = "p"
          }}"""))
      }
      
      intercept[Exception] {
        CredentialConfig(ConfigFactory.parseString("""
            domain = "d"
            username = 12345
            password = "p"
          }}"""))
      }
      
      intercept[Exception] {
        CredentialConfig(ConfigFactory.parseString("""
            domain = "d"
            username = "u"
            password = 12345
          }}"""))
      }
    }
  }
}