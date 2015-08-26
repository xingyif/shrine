package net.shrine.protocol.query

import scala.xml.NodeSeq
import scala.util.Try
import net.shrine.util.NodeSeqEnrichments
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import scala.util.Success
import scala.util.Failure
import net.shrine.util.XmlUtil

/**
 * @author clint
 * @date Jul 23, 2014
 */
final case class Modifiers(name: String, appliedPath: String, key: String) {
  def toI2b2: NodeSeq = XmlUtil.stripWhitespace {
    <constrain_by_modifier>
      <modifier_name>{ name }</modifier_name>
      <applied_path>{ appliedPath }</applied_path>
      <modifier_key>{ key }</modifier_key>
    </constrain_by_modifier>
  }

  def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <modifier>
      <name>{ name }</name>
      <appliedPath>{ appliedPath }</appliedPath>
      <key>{ key }</key>
    </modifier>
  }

  def toJson: JValue = {
    ("name" -> name) ~ ("appliedPath" -> appliedPath) ~ ("key" -> key)
  }

  def transformKey(f: String => Try[String]): Try[Modifiers] = f(key) match {
    case Success(transformed) => Success(copy(key = transformed))
    case Failure(e) => Failure(e)
  }

  //def transformKey(f: String => String): Modifiers = copy(key = f(key))
}

object Modifiers {
  def fromI2b2(xml: NodeSeq): Try[Modifiers] = unmarshalXml(xml, "modifier_name", "applied_path", "modifier_key")

  def fromXml(xml: NodeSeq): Try[Modifiers] = unmarshalXml(xml, "name", "appliedPath", "key")

  private def unmarshalXml(xml: NodeSeq, nameTagName: String, appliedPathTagName: String, keyTagName: String): Try[Modifiers] = {
    import NodeSeqEnrichments.Strictness._

    def text(attempt: Try[NodeSeq]) = attempt.map(_.text)

    for {
      name <- text(xml withChild nameTagName)
      appliedPath <- text(xml withChild appliedPathTagName)
      key <- text(xml withChild keyTagName)
    } yield Modifiers(name, appliedPath, key)
  }

  def fromJson(json: JValue): Try[Modifiers] = {
    import JsonEnrichments._

    for {
      name <- json.withChildString("name")
      appliedPath <- json.withChildString("appliedPath")
      key <- json.withChildString("key")
    } yield Modifiers(name, appliedPath, key)
  }
}