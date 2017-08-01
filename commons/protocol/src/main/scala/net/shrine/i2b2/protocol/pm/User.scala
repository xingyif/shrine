package net.shrine.i2b2.protocol.pm

import net.shrine.serialization.I2b2Unmarshaller
import xml.NodeSeq
import net.shrine.protocol.Credential
import net.shrine.protocol.AuthenticationInfo
import scala.util.{Success, Failure, Try}
import net.shrine.util.{MissingChildNodeException, NodeSeqEnrichments, XmlUtil}

/**
 * @author Bill Simons
 * @since 3/6/12
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
final case class User(fullName: String,
                       username: String,
                       domain: String,
                       credential: Credential,
                       params: Map[String, String],
                       rolesByProject: Map[String, Set[String]]) {

  def toAuthInfo = AuthenticationInfo(domain, username, credential)

  def ecommonsUsername: Option[String] = params.get("ecommons_username")

  def sameUserAs(username:String,domain:String): Boolean = username == this.username && domain == this.domain
}

object User extends I2b2Unmarshaller[Try[User]] {
  object Roles {
    val Manager = "MANAGER"
  }

  override def fromI2b2(nodeSeq: NodeSeq): Try[User] = {

    import NodeSeqEnrichments.Strictness._

    //todo try a transform like below
    val userXml:Try[NodeSeq] = nodeSeq.withChild("message_body").withChild("configure").withChild("user") match {
      case Failure(x) => x match {
        case pmSaysNo:MissingChildNodeException => Failure(BadUsernameOrPasswordException(pmSaysNo))
        case _ => Failure(x)
      }
      case Success(y) => Success(y)
    }

    val projectsXml: Try[NodeSeq] = {
      userXml.withChild("project").transform(s => Success(s),{
        case x:MissingChildNodeException if x.nodeName == "project" => Failure(PmUserWithoutProjectException(userXml,x))
        case x => Failure(x)
      })
    }

    //Parse <param>s that are children of the <user> element.
    //This does not appear to be in line with the i2b2 XSDs, but
    //it reflects what deployed systems actually return.
    val paramsAttempt: Try[Map[String, String]] = {
      def toNameTuple(xmlFragment: NodeSeq): (String, String) = {
        (xmlFragment \ "@name").text -> xmlFragment.text
      }

      for {
        userXml <- userXml
        tuples = (userXml \ "param").map(toNameTuple)
      } yield tuples.toMap
    }

    import XmlUtil.trim

    val rolesByProjectAttempt: Try[Map[String, Set[String]]] = {
      def toProjectRolesTuple(xmlFragment: NodeSeq): (String, Set[String]) = {
        val roles = (xmlFragment \ "role").map(trim).toSet

        trim(xmlFragment \ "@id") -> roles
      }

      for {
        projectsXml <- projectsXml
        tuples = projectsXml.map(toProjectRolesTuple)
      } yield tuples.toMap
    }

    for {
      fullName <- userXml.withChild("full_name").map(trim)
      userName <- userXml.withChild("user_name").map(trim)
      domain <- userXml.withChild("domain").map(trim)
      credential <- userXml.withChild("password").flatMap(Credential.fromI2b2)
      params <-paramsAttempt
      rolesByProject <- rolesByProjectAttempt
    } yield {
      User(fullName, userName, domain, credential, params, rolesByProject)
    }
  }
}

case class BadUsernameOrPasswordException(x:MissingChildNodeException) extends Exception("PM did not recognize username or password: "+x.getMessage,x)

case class PmUserWithoutProjectException(xml:Try[NodeSeq],x:MissingChildNodeException) extends Exception("PM response does not contain a project for this user: "+xml)