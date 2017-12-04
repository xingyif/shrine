package net.shrine.log

/**
 * Censors out things inappropriate for logs, such as i2b2's password element.
 *
 * @author david 
 * @since 8/4/15
 */
object LogCensor {

  /**
   * Matches for things like
   * <password is_token="false" token_ms_timeout="1800000">kapow</password>
   */
  val i2b2PasswordRegex = """(<password.*>).*(</password>)""".r

  /**
   * Matches Base64 strings.
   * From http://stackoverflow.com/questions/475074/regex-to-parse-or-validate-base64-data .
   */
  val base64String = """(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?"""

  /**
   * Matches for things like
   *
   * Authorization: Basic cWVwOnRydXN0bWU=, and Authorization: Basic cWVwOnRydXN0bWU=)
   */
  val basicAuthRegex = s"""(Authorization: Basic )$base64String([,)].*)""".r

  /**
    * Matches for json-ish strings from Typsesafe Config like
    *
    * "password" : "demouser",
    *
    * and
    *
    * "qepPassword" : "demouser",
    */
  val typesafeConfigRegex = """((?i)password" : ").*(",)""".r

  def censor(target:String):String = {
    val magicRedactedReplacement = "$1REDACTED$2"

    val noIb2Passwords = i2b2PasswordRegex.replaceAllIn(target,magicRedactedReplacement)
    val noBasicAuth = basicAuthRegex.replaceAllIn(noIb2Passwords,magicRedactedReplacement)
    typesafeConfigRegex.replaceAllIn(noBasicAuth,magicRedactedReplacement)
  }
}

import org.apache.log4j.PatternLayout
import org.apache.log4j.helpers.PatternParser
import org.apache.log4j.pattern.{FormattingInfo, LiteralPatternConverter, LoggingEventPatternConverter, PatternConverter}
import org.apache.log4j.spi.LoggingEvent

class PasswordCensorMessagePatternConverter extends LoggingEventPatternConverter("PasswordCensorMessage","noPasswordsInMessage") {

  override def format(event: LoggingEvent, toAppendTo: StringBuffer): Unit = {
    val message = LogCensor.censor(event.getRenderedMessage)
    toAppendTo.append(message)
  }

  override def handlesThrowable = true
}

object PasswordCensorMessagePatternConverter {

  def newInstance(options:Array[String]):PasswordCensorMessagePatternConverter = new PasswordCensorMessagePatternConverter
}

class PasswordCensorThrowablePatternConverter extends LoggingEventPatternConverter("PasswordCensorThrowable","noPasswordsInThrowable") {

  override def format(event: LoggingEvent, toAppendTo: StringBuffer): Unit = {

    val throwableInformation = Option(event.getThrowableInformation)

    throwableInformation.fold(()){information =>
      val strings: Array[String] = information.getThrowableStrRep
      strings.map(LogCensor.censor).foreach(toAppendTo.append(_).append("\n"))
    }
  }

  override def handlesThrowable = true
}

object PasswordCensorThrowablePatternConverter {

  def newInstance(options:Array[String]):PasswordCensorThrowablePatternConverter = new PasswordCensorThrowablePatternConverter
}

/**
 * A custom PatternConverter based on BridgePatternConverter to replace the message and throwable sections of pattern.PatternParser
 *
 */
class CustomPatternConverter(pattern:String) extends org.apache.log4j.helpers.PatternConverter {

  import java.util.ArrayList

  import org.apache.log4j.pattern.{PatternParser => ConverterSelector}

  import scala.collection.JavaConverters.{collectionAsScalaIterableConverter, mapAsJavaMapConverter}

  val converterRegistry = {
    val customRules = Map("m" -> classOf[PasswordCensorMessagePatternConverter],
      "message" -> classOf[PasswordCensorMessagePatternConverter],
      "throwable" -> classOf[PasswordCensorThrowablePatternConverter]
    )
    customRules.asJava
  }

  val converters = new ArrayList[PatternConverter]()
  val fields = new ArrayList[FormattingInfo]()

  //parse() fills up converters and fields based on the pattern. Very 1999
  ConverterSelector.parse(pattern,converters,fields,converterRegistry,ConverterSelector.getPatternLayoutRules)

  val patternConverters: Array[PatternConverter] = converters.asScala.map{
    case lepc:LoggingEventPatternConverter => lepc
    case _ => new LiteralPatternConverter("")
  }.toArray[PatternConverter]
  val patternFields: Array[FormattingInfo] = fields.asScala.toArray.padTo(patternConverters.length,FormattingInfo.getDefault)

  val handlesExceptions = true

  val convertersAndFields = patternConverters.zip(patternFields)

  protected def convert(event: LoggingEvent): String = {
    val sbuf: StringBuffer = new StringBuffer
    format(sbuf, event)
    sbuf.toString
  }


  override def format(sbuf: StringBuffer, e: LoggingEvent):Unit = {

    convertersAndFields.foreach{ converterAndField =>
      val startField = sbuf.length()
      converterAndField._1.format(e,sbuf)
      converterAndField._2.format(startField,sbuf)
    }
  }

  def ignoresThrowable: Boolean = {
    !handlesExceptions
  }

}

class CustomPatternParser(pattern:String) extends PatternParser(pattern) {

  override def parse = new CustomPatternConverter(pattern)
}

class CustomPatternLayout extends PatternLayout {

  protected override def createPatternParser(pattern:String):PatternParser = new CustomPatternParser(pattern)

  override def ignoresThrowable(): Boolean = false

  override def activateOptions(): Unit = {}
}