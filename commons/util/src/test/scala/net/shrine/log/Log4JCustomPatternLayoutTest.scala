package net.shrine.log

import java.io.StringWriter

import net.shrine.util.ShouldMatchersForJUnit
import org.apache.log4j.WriterAppender
import org.junit.{After, Before, Test}

/**
 * @author david 
 * @since 8/4/15
 */
class Log4JCustomPatternLayoutTest extends ShouldMatchersForJUnit {

  var writer: StringWriter = null
  var writerAppender: WriterAppender = null
  val layout = new CustomPatternLayout
  layout.setConversionPattern("%d{yyyy-MMM-dd-HH:mm:ss.SSS} %p [SHRINE][%c{1}][%t] %m %n %throwable")

  //todo def testWithWriter higher order function

  @Before
  def setUpLog4J() = {
    writer = new StringWriter()

    writerAppender = new WriterAppender(layout,writer)
    Log.logger.addAppender(writerAppender)
  }

  @After
  def tearDownLog4J() = {
    Log.logger.removeAppender(writerAppender)
  }

  val i2b2PasswordString = """<password is_token="false" token_ms_timeout="1800000">kapow</password>"""
  val expectedI2b2PasswordString = """<password is_token="false" token_ms_timeout="1800000">REDACTED</password>"""

  @Test
  def testCensorI2b2Message() = {

    Log.debug(i2b2PasswordString)
    val result = writer.getBuffer.toString

    assert(result.contains(expectedI2b2PasswordString))
  }

  @Test
  def testCensorI2b2Throwable() = {
    val exception = new Exception(i2b2PasswordString)

    Log.debug("I2B2 sent back some strange xml",exception)
    val result = writer.getBuffer.toString

    assert(result.contains(expectedI2b2PasswordString))
  }

}

