package net.shrine.client

import java.net.URL

import com.typesafe.config.ConfigFactory
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @since Dec 5, 2013
 */
final class EndpointConfigTest extends ShouldMatchersForJUnit {
  @Test
  def testApply {
    def url(s: String) = new URL(s)
    
    intercept[Exception] {
      EndpointConfig(ConfigFactory.empty)
    }

    {
      val configText = """
      foo {
        bar = 123
        baz = 456
        fooEndpoint {
          url = "http://example.com"
          acceptAllCerts = true
          timeout {
            days = 123
          }
        }
      }"""

      import scala.concurrent.duration._
        
      val endpoint = EndpointConfig(ConfigFactory.parseString(configText).getConfig("foo.fooEndpoint"))

      endpoint.url should equal(url("http://example.com"))
      endpoint.acceptAllCerts should be(true)
      endpoint.timeout should equal(123.days)
    }
    
    //Omit acceptAllCerts
    {
      val endpoint = EndpointConfig(ConfigFactory.parseString("""url = "http://example.com""""))
        
      endpoint.url should equal(url("http://example.com"))
      endpoint.acceptAllCerts should be(false)
      endpoint.timeout should equal(EndpointConfig.defaultTimeout)
    }
    
    //Bogus acceptAllCerts
    intercept[Exception] {
      EndpointConfig(ConfigFactory.parseString("""
          url = "http://example.com"
          acceptAllCerts = ";slajfdlkjaf"
          """))
    }
    
    //Bogus URL
    intercept[Exception] {
      EndpointConfig(ConfigFactory.parseString("""
          url = "aslkfjlasfjlkasf"
          acceptAllCerts = true
          """))
    }
    
    //Bogus Timeout
    intercept[Exception] {
      EndpointConfig(ConfigFactory.parseString("""
          url = "http://example.com"
          acceptAllCerts = true
          timeout { askdljalksdj = 1234545 }
          """))
    }
  }
}

object EndpointConfigTest {
  import scala.concurrent.duration._
  def endpoint(url: String): EndpointConfig = EndpointConfig(new URL(url), true, 1.second)
}