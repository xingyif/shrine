package net.shrine.protocol

import scala.xml.NodeSeq

/**
 * @author clint
 * @date Apr 30, 2013
 */
trait NonI2b2ableResponse { self: ShrineResponse =>
  //Fail loudly here
  protected override def i2b2MessageBody: NodeSeq = ???

  override def toI2b2: NodeSeq = ErrorResponse(s"${ getClass.getSimpleName } can't be marshalled to i2b2 XML, as it has no i2b2 analog").toI2b2
}