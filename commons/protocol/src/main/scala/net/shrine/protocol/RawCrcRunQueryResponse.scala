package net.shrine.protocol

import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.protocol.query.QueryDefinition
import scala.xml.NodeSeq
import net.shrine.serialization.{I2b2Unmarshaller, XmlUnmarshaller}

/**
 * @author clint
 * @date Nov 30, 2012
 */
final case class RawCrcRunQueryResponse(
    override val queryId: Long,
    override val createDate: XMLGregorianCalendar,
    override val userId: String,
    override val groupId: String,
    override val requestXml: QueryDefinition,
    override val queryInstanceId: Long,
    val singleNodeResults: Map[ResultOutputType, Seq[QueryResult]]) extends AbstractRunQueryResponse("rawCrcRunQueryResponse", queryId, createDate, userId, groupId, requestXml, queryInstanceId) {

  override def results = singleNodeResults.values.flatten.toSeq
  
  //NB: Will fail loudly if no PATIENT_COUNT_XML or ERROR QueryResults are present; PATIENT_COUNT_XML results take priority
  def toRunQueryResponse: RunQueryResponse = {
    import ResultOutputType.{PATIENT_COUNT_XML, ERROR}
    
    val queryResult = {
      val relevantQueryResults = (singleNodeResults.get(PATIENT_COUNT_XML) orElse singleNodeResults.get(ERROR))
      
      relevantQueryResults.get.head
    }
    
    RunQueryResponse(queryId, createDate, userId, groupId, requestXml, queryInstanceId, queryResult)
  }
  
  private def clearResults = this.copy(singleNodeResults = Map.empty)
  
  import RawCrcRunQueryResponse._
  
  def withResults(results: Iterable[QueryResult]): RawCrcRunQueryResponse = this.copy(singleNodeResults = toQueryResultMap(results))
}

object RawCrcRunQueryResponse extends AbstractRunQueryResponse.Companion[RawCrcRunQueryResponse] {
  import ResultOutputType._
  
  def toQueryResultMap(results: Iterable[QueryResult]): Map[ResultOutputType, Seq[QueryResult]] = {
    results.groupBy(_.resultType.getOrElse(ERROR)).mapValues(_.toSeq)
  }
}