package net.shrine.broadcaster

import java.net.URL

import com.typesafe.config.ConfigFactory
import net.shrine.protocol.NodeId
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

import scala.util.control.NonFatal

/**
 * @author clint
 * @since Dec 5, 2013
 */
final class NodeListParserTest extends ShouldMatchersForJUnit {
  private def url(s: String) = new URL(s)

  @Test
  def testApply {
    NodeListParser(ConfigFactory.empty) should equal(Nil)

    {
      val parsed = NodeListParser(ConfigFactory.parseString("""
        "some hospital somewhere" = "http://example.com"
        CHB = "http://example.com/chb"
        PHS = "http://example.com/phs"
        """)).toSet

      parsed should equal(Set(
        IdAndUrl(NodeId("some hospital somewhere"), url("http://example.com")),
        IdAndUrl(NodeId("CHB"), url("http://example.com/chb")),
        IdAndUrl(NodeId("PHS"), url("http://example.com/phs"))))
    }

    //bogus URLs
    try {
      NodeListParser(ConfigFactory.parseString("""
        "some hospital somewhere" = "asdf"
        CHB = "1234"
        PHS = "blarg"
        """))

      fail("Should have thrown")
    } catch {
      case NonFatal(e) => {
        e.getMessage.contains("asdf") should be(true)
        e.getMessage.contains("1234") should be(true)
        e.getMessage.contains("blarg") should be(true)
      }
    }
  }
}

object NodeListParserTest {

  def node(name: String, url: String): IdAndUrl = IdAndUrl(NodeId(name), new URL(url))

}