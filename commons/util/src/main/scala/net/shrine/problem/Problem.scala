package net.shrine.problem

import java.net.{InetAddress, ConnectException}
import java.util.Date

import net.shrine.log.Loggable
import net.shrine.serialization.{XmlUnmarshaller, XmlMarshaller}

import scala.xml.{Node, NodeSeq}

/**
 * Describes what information we have about a problem at the site in code where we discover it.
 *
 * @author david 
 * @since 8/6/15
 */
trait Problem {
  def summary:String

  def problemName = getClass.getName

  def throwable:Option[Throwable] = None

  def stamp:Stamp

  def description = s"$summary ${stamp.pretty}"

  def throwableDetail = throwable.map(x => x.getStackTrace.mkString(sys.props("line.separator")))

  def details:String = s"$description ${throwableDetail.getOrElse("")}"

  def toDigest:ProblemDigest = ProblemDigest(problemName,summary,description,details)

}

case class ProblemDigest(codec:String,summary:String,description:String,details:String) extends XmlMarshaller {
  override def toXml: Node = {
    <problem>
      <codec>{codec}</codec>
      <summary>{summary}</summary>
      <description>{description}</description>
      <details>{details}</details>
    </problem>
  }
}

object ProblemDigest extends XmlUnmarshaller[ProblemDigest] with Loggable {
  def apply(oldMessage:String):ProblemDigest = {
    val ex = new IllegalStateException(s"'$oldMessage' detected, not in codec. Please report this problem and stack trace to Shrine dev.")
    ex.fillInStackTrace()
    warn(ex)
    ProblemDigest("ProblemNotInCodec",oldMessage,"","")
  }

  override def fromXml(xml: NodeSeq): ProblemDigest = {
    val problemNode = xml \ "problem"
    require(problemNode.nonEmpty,s"No problem tag in $xml")

    def extractText(tagName:String) = {
      val t = (problemNode \ tagName).text
      require(t.nonEmpty)
      t
    }

    val codec = extractText("codec")
    val summary = extractText("summary")
    val description = extractText("description")
    val details = extractText("details")

    ProblemDigest(codec,summary,description,details)
  }
}

case class Stamp(host:InetAddress,time:Long,source:ProblemSources.ProblemSource) {
  def pretty = s"at ${new Date(time)} on $host ${source.pretty}"
}

object Stamp {
  def apply(source:ProblemSources.ProblemSource): Stamp = Stamp(InetAddress.getLocalHost,System.currentTimeMillis(),source)
}

abstract class AbstractProblem(source:ProblemSources.ProblemSource) extends Problem {
  val stamp = Stamp(source)
}

trait ProblemHandler {
  def handleProblem(problem:Problem)
}

/**
 * An example problem handler
 */
object LoggingProblemHandler extends ProblemHandler with Loggable {
  override def handleProblem(problem: Problem): Unit = {

    problem.throwable.fold(error(problem.toString))(throwable =>
      error(problem.toString,throwable)
    )
  }
}

object ProblemSources{

  sealed trait ProblemSource {
//todo name without $
    def pretty = getClass.getSimpleName
  }

  case object Adapter extends ProblemSource
  case object Hub extends ProblemSource
  case object Qep extends ProblemSource
  case object Dsa extends ProblemSource
  case object Unknown extends ProblemSource

  def problemSources = Set(Adapter,Hub,Qep,Dsa,Unknown)
}


case class ProblemNotInCodec(summary:String,t:Throwable) extends AbstractProblem(ProblemSources.Unknown){
  override val throwable = Some(t)

  override val description = s"${super.description} . This error is not yet in the codec. Please report the stack trace to the Shrine development team at TODO"
}

object ProblemNotInCodec {

  def apply(summary:String):ProblemNotInCodec = {
      val x = new IllegalStateException(s"$summary , is not yet in the codec.")
      x.fillInStackTrace()
    new ProblemNotInCodec(summary,x)
  }
}

/**
 * For "Failure querying node 'SITE NAME': java.net.ConnectException: Connection refused"
 *
 * This one is interesting because "Connection refused" is different from "Connection timed out" according to Keith's
 * notes, but the only way to pick that up is to pull the text out of that contained exception. However, all four options
 * are probably worth checking no matter what the exception's message.
 */

//todo NodeId is in protocol, which will be accessible from the hub code where this class should live

//case class CouldNotConnectToQueryNode(nodeId:NodeId,connectExcepition:ConnectException) extends Problem {
case class CouldNotConnectToNode(nodeName:String,connectException:ConnectException) extends AbstractProblem(ProblemSources.Hub) {

  val summary = s"Could not connect to node $nodeName"

  override def throwable = Some(connectException)
}
