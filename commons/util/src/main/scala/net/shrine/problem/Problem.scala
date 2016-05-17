package net.shrine.problem

import java.net.InetAddress
import java.util.Date

import net.shrine.log.Loggable
import net.shrine.serialization.{XmlUnmarshaller, XmlMarshaller}

import scala.xml.{Elem, Node, NodeSeq}

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

  def description:String

  def exceptionXml(exception:Option[Throwable]): Option[Elem] = exception.map{x =>
    <exception>
      <name>{x.getClass.getName}</name>
      <message>{x.getMessage}</message>
      <stacktrace>
        {x.getStackTrace.map(line => <line>{line}</line>)}{exceptionXml(Option(x.getCause)).getOrElse("")}
      </stacktrace>
    </exception>
  }

  def throwableDetail = exceptionXml(throwable)

  def detailsXml: NodeSeq = NodeSeq.fromSeq(<details>{throwableDetail.getOrElse("")}</details>)

  def toDigest:ProblemDigest = ProblemDigest(problemName,stamp.pretty,summary,description,detailsXml)

}

case class ProblemDigest(codec: String, stampText: String, summary: String, description: String, detailsXml: NodeSeq) extends XmlMarshaller {

  override def toXml: Node = {
    <problem>
      <codec>{codec}</codec>
      <stamp>{stampText}</stamp>
      <summary>{summary}</summary>
      <description>{description}</description>
      {detailsXml}
    </problem>
  }

  /**
   * Ignores detailXml. equals with scala.xml is impossible. See http://www.scala-lang.org/api/2.10.3/index.html#scala.xml.Equality$
   */
  override def equals(other: Any): Boolean =
    other match {

      case that: ProblemDigest =>
        (that canEqual this) &&
          codec == that.codec &&
          stampText == that.stampText &&
          summary == that.summary &&
          description == that.description
      case _ => false
    }

  /**
   * Ignores detailXml
   */
  override def hashCode: Int = {
    val prime = 67
    codec.hashCode + prime * (stampText.hashCode + prime *(summary.hashCode + prime * description.hashCode))
  }
}

object ProblemDigest extends XmlUnmarshaller[ProblemDigest] with Loggable {

  override def fromXml(xml: NodeSeq): ProblemDigest = {
    val problemNode = xml \ "problem"
    require(problemNode.nonEmpty,s"No problem tag in $xml")

    def extractText(tagName:String) = (problemNode \ tagName).text

    val codec = extractText("codec")
    val stampText = extractText("stamp")
    val summary = extractText("summary")
    val description = extractText("description")
    val detailsXml: NodeSeq = problemNode \ "details"

    ProblemDigest(codec,stampText,summary,description,detailsXml)
  }
}

case class Stamp(host:InetAddress,time:Long,source:ProblemSources.ProblemSource) {
  def pretty = s"${new Date(time)} on ${host.getHostName} ${source.pretty}"
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
    def pretty = getClass.getSimpleName.dropRight(1)
  }

  case object Adapter extends ProblemSource
  case object Hub extends ProblemSource
  case object Qep extends ProblemSource
  case object Dsa extends ProblemSource
  case object Unknown extends ProblemSource

  def problemSources = Set(Adapter,Hub,Qep,Dsa,Unknown)
}

case class ProblemNotYetEncoded(internalSummary:String,t:Option[Throwable] = None) extends AbstractProblem(ProblemSources.Unknown){
  override val summary = "An unanticipated problem encountered."

  override val throwable = {
    val rx = t.fold(new IllegalStateException(s"$summary"))(
      new IllegalStateException(s"$summary",_)
      )
    rx.fillInStackTrace()
    Some(rx)
  }

  val reportedAtStackTrace = new IllegalStateException("Capture reporting stack trace.")

  override val description = "This problem is not yet classified in Shrine source code. Please report the details to the Shrine dev team."

  override val detailsXml: NodeSeq = NodeSeq.fromSeq(
    <details>
      {internalSummary}
      {throwableDetail.getOrElse("")}
    </details>
  )
}

object ProblemNotYetEncoded {
  def apply(summary:String,x:Throwable):ProblemNotYetEncoded = ProblemNotYetEncoded(summary,Some(x))
}
