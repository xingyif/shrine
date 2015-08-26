package net.shrine.protocol

import scala.xml.NodeSeq
import scala.util.Try
import scala.util.control.NonFatal

/**
 * @author clint
 * @date Feb 14, 2014
 */
object XmlUnmarshallers {
  type Unmarshaller[R] = Set[ResultOutputType] => NodeSeq => Try[R]
  
  private final class CompositeUnmarshaller[R](lhs: Unmarshaller[R], rhs: Unmarshaller[R]) extends Unmarshaller[R] {
    override def apply(breakdownTypes: Set[ResultOutputType]): NodeSeq => Try[R] = { xml =>
      lhs(breakdownTypes)(xml).recoverWith {
        case NonFatal(e) => rhs(breakdownTypes)(xml)
      }
    }
  }

  abstract class Chained[+R](unmarshallers: Unmarshaller[R]*) {
    private lazy val delegate = unmarshallers.reduce { new CompositeUnmarshaller(_, _) }
    
    def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[R] = delegate(breakdownTypes)(xml)
  }
}