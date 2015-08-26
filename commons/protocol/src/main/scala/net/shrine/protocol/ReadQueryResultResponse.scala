package net.shrine.protocol

import net.shrine.serialization.XmlMarshaller
import net.shrine.util.XmlUtil
import scala.xml.NodeSeq
import net.shrine.serialization.XmlUnmarshaller
import scala.util.Try

/**
 * @author clint
 * @date Nov 2, 2012
 */
final case class ReadQueryResultResponse(
    override val queryId: Long, 
    val singleNodeResult: QueryResult) extends AbstractReadQueryResultResponse("readQueryResultResponse", queryId) {
  
  override def results = Seq(singleNodeResult)
}

object ReadQueryResultResponse extends AbstractReadQueryResultResponse.Companion((id, results) => new ReadQueryResultResponse(id, results.head))
