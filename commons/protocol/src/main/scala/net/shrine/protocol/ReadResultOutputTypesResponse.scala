package net.shrine.protocol

import scala.xml.NodeSeq
import net.shrine.util.XmlUtil

/**
 * @author clint
 * @date Oct 6, 2014
 * 
 * NB: Takes a Seq of ResultOutputTypes, even though this should probably be a Set to disallow dupes, for
 * better determinism when testing.
 */
final case class ReadResultOutputTypesResponse(outputTypes: Seq[ResultOutputType]) extends ShrineResponse {
  override protected[protocol] def i2b2MessageBody: NodeSeq = XmlUtil.stripWhitespace {
    <ns4:response xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns4:result_type_responseType">
      <status>
        <condition type="DONE">DONE</condition>
      </status>
      {
        outputTypes.zipWithIndex.map {
          case (outputType, index) =>
            outputType.withId(index + 1).toI2b2
        }
      }
    </ns4:response>
  }

  //NB: Not used anywhere, but perhaps useful for logging
  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <resultOutputTypes>
			{
        outputTypes.map(ot => <resultOutputType>{ ot }</resultOutputType>)
			}
	  </resultOutputTypes>
  }
}

