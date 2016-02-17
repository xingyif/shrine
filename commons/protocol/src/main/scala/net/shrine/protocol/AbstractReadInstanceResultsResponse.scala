package net.shrine.protocol

import scala.xml.NodeSeq

import net.shrine.util.XmlUtil

/**
 * @author clint
 * @since Nov 30, 2012
 */
abstract class AbstractReadInstanceResultsResponse(
    rootTagName: String,
    /*
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * NOTE: Now that the adapter caches/stores results from the CRC, Instead of an
     * i2b2 instance id, this class now contains the Shrine-generated, network-wide 
     * id of a query, which is used to obtain results previously obtained from the 
     * CRC from Shrine's datastore.
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */
    val shrineNetworkQueryId: Long) extends ShrineResponse with HasQueryResults {

  //NB: Set QueryResults' instanceIds to the query id of the enclosing response
  private def resultsWithNetworkQueryId = results.map(_.withInstanceId(shrineNetworkQueryId))

  override protected final def i2b2MessageBody = XmlUtil.stripWhitespace {
    <ns5:response xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns5:result_responseType">
      <status>
        <condition type="DONE">DONE</condition>
      </status>
      { resultsWithNetworkQueryId.map(_.toI2b2) }
    </ns5:response>
  }

  override final def toXml = XmlUtil.stripWhitespace {
    XmlUtil.renameRootTag(rootTagName) {
      <placeHolder>
        <shrineNetworkQueryId>{ shrineNetworkQueryId }</shrineNetworkQueryId>
        <queryResults>
          { resultsWithNetworkQueryId.map(_.toXml) }
        </queryResults>
      </placeHolder>
    }
  }
}

object AbstractReadInstanceResultsResponse {
  //
  //NB: Creatable trait and companion object implement the typeclass pattern:
  //http://www.casualmiracles.com/2012/05/03/a-small-example-of-the-typeclass-pattern-in-scala/
  //A typeclass is used here in place of an abstract method with multiple concrete implementations,
  //or another similar strategy. -Clint
  //todo do something simpler and easier to explain.

  private trait Creatable[T] {
    def apply(id: Long, results: Seq[QueryResult]): T
  }

  private object Creatable {
    implicit val readInstanceResultsResponseIsCreatable: Creatable[ReadInstanceResultsResponse] = new Creatable[ReadInstanceResultsResponse] {
      override def apply(id: Long, results: Seq[QueryResult]) = ReadInstanceResultsResponse(id, results.head)
    }

    implicit val aggregatedReadInstanceResultsResponseIsCreatable: Creatable[AggregatedReadInstanceResultsResponse] = new Creatable[AggregatedReadInstanceResultsResponse] {
      override def apply(id: Long, results: Seq[QueryResult]) = AggregatedReadInstanceResultsResponse(id, results)
    }
  }

  abstract class Companion[R <: AbstractReadInstanceResultsResponse: Creatable] {
    private val createResponse = implicitly[Creatable[R]]

    def fromI2b2(breakdownTypes: Set[ResultOutputType])(nodeSeq: NodeSeq): R = {
      val results = (nodeSeq \ "message_body" \ "response" \ "query_result_instance").map(QueryResult.fromI2b2(breakdownTypes))

      //TODO - parsing error if no results - need to deal with "no result" cases
      val shrineNetworkQueryId = results.head.instanceId

      createResponse(shrineNetworkQueryId, results)
    }

    def fromXml(breakdownTypes: Set[ResultOutputType])(nodeSeq: NodeSeq): R = {
      val shrineNetworkQueryId = (nodeSeq \ "shrineNetworkQueryId").text.toLong

      val results = (nodeSeq \ "queryResults" \ "_").map(QueryResult.fromXml(breakdownTypes))

      createResponse(shrineNetworkQueryId, results)
    }
  }
}