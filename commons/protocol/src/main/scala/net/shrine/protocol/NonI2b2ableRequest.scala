package net.shrine.protocol

import scala.xml.NodeSeq

/**
 * @author clint
 * @date Mar 29, 2013
 */
trait NonI2b2ableRequest { self: ShrineRequest =>
  //NB: Intentionally left out
  override def toI2b2 = ???

  //NB: intentionally left unimplemented
  protected def i2b2MessageBody: NodeSeq = ???
}