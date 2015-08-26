package net.shrine.serialization

import xml.NodeSeq

/**
 * @author Bill Simons
 * @date 3/24/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
trait I2b2Marshaller {
  def toI2b2: NodeSeq

  def toI2b2String: String = toI2b2.toString
}