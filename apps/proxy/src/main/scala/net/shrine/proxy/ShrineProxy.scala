package net.shrine.proxy

import net.shrine.log.Loggable

import scala.xml.XML
import scala.xml.NodeSeq
import net.shrine.client.JerseyHttpClient
import net.shrine.client.HttpResponse
import net.shrine.crypto.TrustParam.AcceptAllCerts
import net.shrine.client.HttpClient

/**
 * [ Author ]
 *
 * @author Clint Gilbert
 * @author Ricardo Delima
 * @author Andrew McMurry
 * @author Britt Fitch
 *         <p/>
 *         Date: Apr 1, 2008
 *         Harvard Medical School Center for BioMedical Informatics
 * @link http://cbmi.med.harvard.edu
 * <p/>
 * NB: In the previous version of this class, the black list had no effect on the result of calling
 * isAllawableDomain (now isAllowableUrl).  This behavior is preserved here. -Clint
 *
 */
trait ShrineProxy {
  def isAllowableUrl(redirectURL: String): Boolean

  def redirect(request: NodeSeq): HttpResponse
}

object DefaultShrineProxy extends Loggable {

  private[proxy] def loadWhiteList: Set[String] = loadList("whitelist")

  private[proxy] def loadBlackList: Set[String] = loadList("blacklist")

  private def loadList(listname: String): Set[String] = {
    val confFile = getClass.getClassLoader.getResource("shrine-proxy-acl.xml").getFile

    val confXml = XML.loadFile(confFile)

    (confXml \\ "lists" \ listname \ "host").map(_.text.trim).toSet
  }

  val jerseyHttpClient: HttpClient = {
    import scala.concurrent.duration._
    
    //TODO: Make timeout configurable?
    JerseyHttpClient(AcceptAllCerts, Duration.Inf)
  }
}

final class DefaultShrineProxy(val whiteList: Set[String], val blackList: Set[String], val httpClient: HttpClient) extends ShrineProxy with Loggable {

  def this() = this(DefaultShrineProxy.loadWhiteList, DefaultShrineProxy.loadBlackList, DefaultShrineProxy.jerseyHttpClient)

  import DefaultShrineProxy._

  whiteList.foreach(entry => info(s"Whitelist entry: $entry"))
  blackList.foreach(entry => info(s"Blacklist entry: $entry"))

  info("Loaded access control lists.")

  override def isAllowableUrl(redirectURL: String): Boolean = whiteList.exists(redirectURL.startsWith) && !blackList.exists(redirectURL.startsWith)

  /**
   * Redirect to a URL embedded within the I2B2 message
   *
   * @param request a chunk of xml with a <redirect_url> element, containing the url to redirect to.
   * @return the String result of accessing the url embedded in the passed request xml.
   * @throws ShrineMessageFormatException bad input XML
   */
  override def redirect(request: NodeSeq): HttpResponse = {
    val redirectUrl = (request \\ "redirect_url").headOption.map(_.text.trim).getOrElse {
      error("Error parsing redirect_url tag")

      throw new ShrineMessageFormatException("Error parsing redirect_url tag")
    }

    if (redirectUrl == null || redirectUrl.isEmpty) {
      error("Detected missing redirect_url tag")

      throw new ShrineMessageFormatException("ShrineAdapter detected missing redirect_url tag")
    }

    //if redirectURL is not in the white list, do not proceed.
    if (!isAllowableUrl(redirectUrl)) {
      throw new ShrineMessageFormatException(s"redirectURL not in white list or is in black list: $redirectUrl")
    }

    debug(s"Proxy redirecting to $redirectUrl")

    httpClient.post(request.toString, redirectUrl)
  }
}
