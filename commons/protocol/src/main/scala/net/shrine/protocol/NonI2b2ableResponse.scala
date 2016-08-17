package net.shrine.protocol

import net.shrine.problem.{AbstractProblem, ProblemSources}

import scala.xml.NodeSeq

/**
 * @author clint
 * @since Apr 30, 2013
 */
trait NonI2b2ableResponse { self: ShrineResponse =>
  //Fail loudly here
  protected override def i2b2MessageBody: NodeSeq = ???

  override def toI2b2: NodeSeq = ErrorResponse(NoI2b2AnalogExists(this.getClass)).toI2b2
}

case class NoI2b2AnalogExists(claz:Class[_ <: NonI2b2ableResponse]) extends AbstractProblem(ProblemSources.Unknown) {
  override def summary: String = s"${ claz.getSimpleName } can't be marshalled to i2b2 XML, as it has no i2b2 analog"

  override def description: String = s"${ claz.getSimpleName } can't be marshalled to i2b2 XML, as it has no i2b2 analog"
  createAndLog
}