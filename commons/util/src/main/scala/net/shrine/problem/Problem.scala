package net.shrine.problem

import java.net.{InetAddress, ConnectException}

import net.shrine.log.Loggable

/**
 * Describes what information we have about a problem at the site in code where we discover it.
 *
 * @author david 
 * @since 8/6/15
 */
trait Problem {
  def message:String

  def getProblemName = getClass.getName

  def throwable:Option[Throwable] = None

  def source:ProblemSources.ProblemSource

  def stamp:Stamp
}

case class Stamp(host:InetAddress,time:Long)

object Stamp {
  def apply(): Stamp = Stamp(InetAddress.getLocalHost,System.currentTimeMillis())
}

abstract class AbstractProblem(override val source:ProblemSources.ProblemSource) extends Problem {
  val stamp = Stamp()
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

  sealed trait ProblemSource

  case object Adapter extends ProblemSource
  case object Hub extends ProblemSource
  case object Qep extends ProblemSource
  case object Dsa extends ProblemSource

  def problemSources = Set(Adapter,Hub,Qep,Dsa)
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

  val message = s"Could not connect to node $nodeName"

  override def throwable = Some(connectException)
}
