package net.shrine.protocol

import xml.NodeSeq
import net.shrine.util.XmlUtil
import net.shrine.serialization.{ I2b2Marshaller, I2b2Unmarshaller, XmlMarshaller, XmlUnmarshaller }
import scala.util.Try
import net.shrine.util.NodeSeqEnrichments

/**
 * @author Bill Simons
 * @date 3/9/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 *
 * NB: this is a case class to get a structural equality contract in hashCode and equals, mostly for testing
 *
 * NB: Exposes a constructor that takes a String, so that JAXRS can automatically unmarshal an instance of this
 * class from a String
 */
final case class AuthenticationInfo(
  val domain: String,
  val username: String,
  val credential: Credential) extends XmlMarshaller with I2b2Marshaller {

  //NB: For JAXRS 
  private def this(other: AuthenticationInfo) = this(other.domain, other.username, other.credential)

  //NB: For JAXRS
  def this(serializedForm: String) = this(AuthenticationInfo.fromHeader(serializedForm))

  override def toString: String = AuthenticationInfo.elided.rawToString
  
  def rawToString: String = s"${getClass.getSimpleName}($domain,$username,$credential)" 
  
  override def toI2b2 = XmlUtil.stripWhitespace {
    <security>
      <domain>{ domain }</domain>
      <username>{ username }</username>
      { credential.toI2b2 }
    </security>
  }

  def toHeader = {
    import AuthenticationInfo.{ headerPrefix => prefix, headerDelimiter => delim }

    prefix + Seq(domain, username, credential.value, credential.isToken).mkString(delim)
  }

  import AuthenticationInfo.shrineXmlTagName

  override def toXml = XmlUtil.renameRootTag(shrineXmlTagName) {
    XmlUtil.stripWhitespace {
      <placeHolder>
        <domain>{ domain }</domain>
        <username>{ username }</username>
        { credential.toXml }
      </placeHolder>
    }
  }
}

object AuthenticationInfo extends I2b2Unmarshaller[Try[AuthenticationInfo]] with XmlUnmarshaller[Try[AuthenticationInfo]] {

  val elided = AuthenticationInfo("*******", "*******", Credential("*******", false))
  
  val shrineXmlTagName = "authenticationInfo"

  import NodeSeqEnrichments.Strictness._
    
  override def fromI2b2(xml: NodeSeq): Try[AuthenticationInfo] = {
    for {
      domain <- xml.withChild("domain").map(_.text)
      username <- xml.withChild("username").map(_.text)
      credential <- xml.withChild("password").flatMap(Credential.fromI2b2)
    } yield {
      AuthenticationInfo(domain, username, credential)
    }
  }

  override def fromXml(nodeSeq: NodeSeq): Try[AuthenticationInfo] = {
    for {
      domain <- Try((nodeSeq \ "domain").text.trim)
      username <- Try((nodeSeq \ "username").text.trim)
      credential <- Credential.fromXml(nodeSeq \ "credential")
    } yield AuthenticationInfo(domain, username, credential)
  }

  private[protocol] val headerPrefix = "SHRINE "

  private[protocol] val headerDelimiter = ","

  def fromHeader(header: String) = {
    val Array(_, headerValue) = header.split(headerPrefix)

    val Array(domain, username, password, isTokenStr) = headerValue.split(headerDelimiter)

    new AuthenticationInfo(domain, username, new Credential(password, isTokenStr.toBoolean))
  }
}