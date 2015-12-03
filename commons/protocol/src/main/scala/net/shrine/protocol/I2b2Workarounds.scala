package net.shrine.protocol

/**
 * @author clint
 * @date Aug 15, 2012
 */
object I2b2Workarounds {
  def unescape(semiEscapedXml: String): String = {
    semiEscapedXml.replaceAll("&lt;", "<").replaceAll("&amp;gt;", ">")
  }
  
  def escape(xml: String): String = {
    xml.replaceAll("<", "&lt;") //TODO: some >'s should be turned into '&amp;gt;' :( 
  }
}