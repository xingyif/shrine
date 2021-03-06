package net.shrine.protocol

import net.shrine.serialization.XmlMarshaller
import net.shrine.util.XmlUtil
import net.shrine.serialization.XmlUnmarshaller

import scala.util.Try
import scala.xml.{Node, NodeSeq}

/**
 * @author clint
 * @since Nov 1, 2013
 */
final case class NodeId(name: String) extends XmlMarshaller {
  override def toXml: Node = XmlUtil.stripWhitespace {
    import NodeId._

    XmlUtil.renameRootTag(rootTagName) {
      <placeholder>
        <name>{ name }</name>
      </placeholder>
    }
  }
}

object NodeId extends XmlUnmarshaller[Try[NodeId]] {

  val rootTagName = "nodeId"

  val Unknown = NodeId("Unknown")

  override def fromXml(xml: NodeSeq): Try[NodeId] = {
    for {
      name <- Try((xml \ "name").text.trim).filter(!_.isEmpty)
    } yield NodeId(name)
  }

  def fromXmlOption(xml:NodeSeq):Try[Option[NodeId]] = Try {
    val nameElement: NodeSeq = (xml \ "name")
    val contents = nameElement.text
    if (contents.isEmpty) None
    else Some(NodeId(contents))
  }
}
 