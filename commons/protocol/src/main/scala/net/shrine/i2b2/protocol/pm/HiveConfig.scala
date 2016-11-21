package net.shrine.i2b2.protocol.pm

import net.shrine.util.XmlUtil
import net.shrine.serialization.{ I2b2Unmarshaller, XmlMarshaller }
import xml.NodeSeq

/**
 * @author Bill Simons
 * @date 3/5/12
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
//TODO: Do we use this anymore? If it's only in happy, it can be removed
final case class HiveConfig(val crcUrl: String, val ontologyUrl: String) extends XmlMarshaller {
  override def toXml = XmlUtil.stripWhitespace {
    <hiveConfig>
      <crcUrl>{ crcUrl }</crcUrl>
      <ontUrl>{ ontologyUrl }</ontUrl>
    </hiveConfig>
  }
}

object HiveConfig extends I2b2Unmarshaller[HiveConfig] {
  override def fromI2b2(nodeSeq: NodeSeq) = {
    val cellDataSeq = nodeSeq \ "message_body" \ "configure" \ "cell_datas" \ "cell_data"
    
    def hasId(id: String): NodeSeq => Boolean = {
      xml => (xml \\ "@id").text == id
    }
    
    def findUrlById(id: String): String = (cellDataSeq.find(hasId(id)).toSeq \ "url").text
    
    //TODO review for error handling - if given ID isn't found, urls will be empty strings. Should we fail loudly instead?
    val crcUrl = findUrlById("CRC")
    
    val ontUrl = findUrlById("ONT")

    HiveConfig(crcUrl, ontUrl)
  }
}