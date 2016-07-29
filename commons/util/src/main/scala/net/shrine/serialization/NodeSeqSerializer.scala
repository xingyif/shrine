package net.shrine.serialization

import org.json4s.{Formats, MappingException, Serializer, _}
import org.json4s.reflect.TypeInfo
import org.json4s.Xml.{toJson, toXml}

import scala.xml.{NodeSeq, XML}

/**
  * Created by ty on 7/22/16.
  */
// The default json serializer throws a stack overflow when trying to serialize NodeSeq.
class NodeSeqSerializer extends Serializer[NodeSeq] {
  private val NodeSeqClass = classOf[NodeSeq]

  override def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), NodeSeq] = {
    case (TypeInfo(NodeSeqClass, _), json) => toXml(json)
  }
  override def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case node:NodeSeq => toJson(node)
  }
}
