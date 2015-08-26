package net.shrine.protocol

import net.shrine.serialization.XmlMarshaller
import net.shrine.util.{Tries, XmlUtil, XmlDateHelper}
import scala.xml.NodeSeq
import net.shrine.serialization.XmlUnmarshaller
import scala.util.Try

/**
 * @author clint
 * @since Feb 28, 2014
 */
final case class MultiplexedResults(results: Seq[SingleNodeResult]) extends XmlMarshaller {
  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <shrineResults>
      { results.map(_.toXml) } 
    </shrineResults>
  }
}

object MultiplexedResults {
  def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[MultiplexedResults] = {
    val attempts = for {
      resultXml <- xml \ "_"
    } yield SingleNodeResult.fromXml(breakdownTypes)(resultXml)
    
    Tries.sequence(attempts).map(MultiplexedResults(_))
  }
}