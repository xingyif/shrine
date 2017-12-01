package net.shrine.log

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author david
 * @since 7/24/15
 */
class LogCensorTest extends ShouldMatchersForJUnit {

  @Test
  def testCensorI2b2() = {
    val i2b2PasswordString = """<password is_token="false" token_ms_timeout="1800000">kapow</password>"""
    val expectedI2b2PasswordString = """<password is_token="false" token_ms_timeout="1800000">REDACTED</password>"""

    val result = LogCensor.censor(i2b2PasswordString)
    result should be(expectedI2b2PasswordString)
  }

  @Test
  def testCensorBasicAuthWithMore() = {
    val basicAuthLine = "HttpRequest(GET,https://shrine-qa1.hms.harvard.edu:6443/qep/approvedTopics/user/shrine,List(Host: shrine-qa1.hms.harvard.edu:6443, Authorization: Basic cWVwOnRydXN0bWU=, User-Agent: spray-can/1.3.3),Empty,HTTP/1.1)"
    val expectedBasicAuthLine = "HttpRequest(GET,https://shrine-qa1.hms.harvard.edu:6443/qep/approvedTopics/user/shrine,List(Host: shrine-qa1.hms.harvard.edu:6443, Authorization: Basic REDACTED, User-Agent: spray-can/1.3.3),Empty,HTTP/1.1)"

    val result = LogCensor.censor(basicAuthLine)
    result should be(expectedBasicAuthLine)
  }

  //Request: HttpRequest(POST,http://example.com/steward/rejectTopic/topic/1,List(Authorization: Basic ZGF2ZTprYWJsYW0=),Empty,HTTP/1.1)
  @Test
  def testCensorBasicAuthLast() = {
    val basicAuthLine = "Request: HttpRequest(POST,http://example.com/steward/rejectTopic/topic/1,List(Authorization: Basic ZGF2ZTprYWJsYW0=),Empty,HTTP/1.1)"
    val expectedBasicAuthLine = "Request: HttpRequest(POST,http://example.com/steward/rejectTopic/topic/1,List(Authorization: Basic REDACTED),Empty,HTTP/1.1)"

    val result = LogCensor.censor(basicAuthLine)
    result should be(expectedBasicAuthLine)
  }
  
  //"password" : "flarf",
  @Test
  def testCensorTypesafeConfigPassword() = {
    val typesafeConfigLine = "\"password\" : \"flarf\","
    val expectedTypesafeConfigLine = "\"password\" : \"REDACTED\","

    val result = LogCensor.censor(typesafeConfigLine)
    result should be(expectedTypesafeConfigLine)
  }

  //"qepPassword" : "flarf",
  @Test
  def testCensorTypesafeConfigQepPassword() = {
    val typesafeConfigLine = "\"qepPassword\" : \"flarf\","
    val expectedTypesafeConfigLine = "\"qepPassword\" : \"REDACTED\","

    val result = LogCensor.censor(typesafeConfigLine)
    result should be(expectedTypesafeConfigLine)
  }
}

