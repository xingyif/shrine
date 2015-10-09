package net.shrine.problem

import java.net.InetAddress
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

  def description = s"${stamp.pretty}"

  //todo stack trace as xml elements? would be easy
  def throwableDetail = throwable.map(x => s"${x.getClass.getName} ${x.getMessage}\n${x.getStackTrace.mkString(sys.props("line.separator"))}")

  def details:String = s"${throwableDetail.getOrElse("")}"

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

    def extractText(tagName:String) = (problemNode \ tagName).text

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
    def pretty = getClass.getSimpleName.drop(1)
  }

  case object Adapter extends ProblemSource
  case object Hub extends ProblemSource
  case object Qep extends ProblemSource
  case object Dsa extends ProblemSource
  case object Unknown extends ProblemSource

  def problemSources = Set(Adapter,Hub,Qep,Dsa,Unknown)
}


case class ProblemNotYetEncoded(summary:String,t:Throwable) extends AbstractProblem(ProblemSources.Unknown){
  override val throwable = Some(t)

  override val description = s"${super.description} . This error is not yet in the codec. Please report the stack trace to the Shrine development team at TODO"
}

object ProblemNotYetEncoded {

  def apply(summary:String):ProblemNotYetEncoded = {
      val x = new IllegalStateException(s"$summary , is not yet in the codec.")
      x.fillInStackTrace()
    new ProblemNotYetEncoded(summary,x)
  }
}
