package net.shrine.util

/**
 * @author clint
 * @since Oct 18, 2012
 */
object Base64 {
  def toBase64(bytes: Array[Byte]): String = javax.xml.bind.DatatypeConverter.printBase64Binary(bytes)

  def fromBase64(encoded: String): Array[Byte] = javax.xml.bind.DatatypeConverter.parseBase64Binary(encoded)

}
