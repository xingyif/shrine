package net.shrine.client

import net.shrine.log.Loggable

import scala.util.control.NonFatal
import scala.xml.NodeSeq
import scala.xml.XML
import com.sun.jersey.api.client.RequestBuilder
import com.sun.jersey.api.client.UniformInterface
import com.sun.jersey.api.client.WebResource
import JerseyHttpClient.createJerseyClient
import javax.ws.rs.core.MediaType

import net.shrine.crypto.TrustParam
import net.shrine.protocol.{AggregatedReadInstanceResultsResponse, AggregatedReadQueryResultResponse, AggregatedReadTranslatedQueryDefinitionResponse, AggregatedRunQueryResponse, AuthenticationInfo, DeleteQueryResponse, FlagQueryResponse, OutputTypeSet, ReadApprovedQueryTopicsResponse, ReadPdoResponse, ReadPreviousQueriesResponse, ReadQueryDefinitionResponse, ReadQueryInstancesResponse, RenameQueryResponse, ResultOutputType, UnFlagQueryResponse}
import net.shrine.protocol.query.QueryDefinition
import net.shrine.util.UrlCheck
import net.shrine.serialization.XmlUnmarshaller

import scala.util.Try

/**
 *
 * @author Clint Gilbert
 * @since Sep 16, 2011
 *
 * @see http://cbmi.med.harvard.edu
 *
 * A client for remote ShrineResources, implemented using Jersey
 *
 * @param shrineUrl: The base URL that the remote ShrineResource is exposed at
 */
final class JerseyShrineClient(val shrineUrl: String, val projectId: String, val authorization: AuthenticationInfo, breakdownTypes: Set[ResultOutputType], trustParam: TrustParam) extends ShrineClient with Loggable {
  //TODO: Take an endpoint, to allow specifying timeouts
  
  import JerseyShrineClient._
  import Deserializer._
  
  require(shrineUrl != null)
  require(UrlCheck.isValidUrl(shrineUrl))
  require(projectId != null)
  require(authorization != null)

  
  private[client] lazy val webResource = {
    import scala.concurrent.duration._
    
    //TODO: Don't hardcode timeout here
    createJerseyClient(trustParam, 5.minutes).resource(shrineUrl)
  }

  override def flagQuery(networkQueryId: Long, messageOption: Option[String], shouldBroadcast: Boolean): FlagQueryResponse = {
    post[FlagQueryResponse](shouldBroadcast) {
      val resource = webResource.path(s"/shrine/queries/$networkQueryId/flag")
      
      messageOption match {
        case Some(message) => resource.entity(message, MediaType.TEXT_PLAIN)
        case None => resource
      }
    }
  }
  
  override def unFlagQuery(networkQueryId: Long, shouldBroadcast: Boolean): UnFlagQueryResponse = {
    post[UnFlagQueryResponse](shouldBroadcast) {
      webResource.path(s"/shrine/queries/$networkQueryId/unflag")
    }
  }
  
  override def readTranslatedQueryDefinition(queryDefinition: QueryDefinition, shouldBroadcast: Boolean = true): AggregatedReadTranslatedQueryDefinitionResponse = {
    post[AggregatedReadTranslatedQueryDefinitionResponse](shouldBroadcast) {
      webResource.path("/shrine/queries/translated").entity(queryDefinition.toXmlString, MediaType.APPLICATION_XML)
    }
  }
  
  override def readApprovedQueryTopics(userId: String, shouldBroadcast: Boolean = true) = {
    get[ReadApprovedQueryTopicsResponse](shouldBroadcast) {
      webResource.path(s"/shrine/$userId/approved-topics")
    }
  }

  override def readPreviousQueries(userId: String, fetchSize: Int, shouldBroadcast: Boolean = true) = {
    get[ReadPreviousQueriesResponse](shouldBroadcast) {
      webResource.path(s"/shrine/$userId/queries").queryParam("fetchSize", fetchSize.toString)
    }
  }

  override def runQuery(topicId: String, outputTypes: Set[ResultOutputType], queryDefinition: QueryDefinition, shouldBroadcast: Boolean = true) = {
    post[AggregatedRunQueryResponse](shouldBroadcast) {
      webResource.path("/shrine/queries").header("outputTypes", OutputTypeSet(outputTypes).serialized).header("topicId", topicId).entity(queryDefinition.toXmlString, MediaType.APPLICATION_XML)
    }
  }

  override def readQueryInstances(queryId: Long, shouldBroadcast: Boolean = true) = {
    get[ReadQueryInstancesResponse](shouldBroadcast) {
      webResource.path(s"/shrine/queries/$queryId/instances")
    }
  }

  override def readInstanceResults(instanceId: Long, shouldBroadcast: Boolean = true) = {
    get[AggregatedReadInstanceResultsResponse](shouldBroadcast) {
      webResource.path(s"/shrine/instances/$instanceId/results")
    }
  }

  override def readPdo(patientSetCollId: String, optionsXml: NodeSeq, shouldBroadcast: Boolean = true) = {
    post[ReadPdoResponse](shouldBroadcast) {
      webResource.path(s"/shrine/patient-set/$patientSetCollId").entity(optionsXml.toString, MediaType.APPLICATION_XML)
    }
  }

  override def readQueryDefinition(queryId: Long, shouldBroadcast: Boolean = true) = {
    get[ReadQueryDefinitionResponse](shouldBroadcast) {
      webResource.path(s"/shrine/queries/$queryId")
    }
  }

  override def deleteQuery(queryId: Long, shouldBroadcast: Boolean = true) = {
    delete[DeleteQueryResponse](shouldBroadcast) {
      webResource.path(s"/shrine/queries/$queryId")
    }
  }

  override def renameQuery(queryId: Long, queryName: String, shouldBroadcast: Boolean = true) = {
    post[RenameQueryResponse](shouldBroadcast) {
      webResource.path(s"/shrine/queries/$queryId/name").entity(queryName, MediaType.TEXT_PLAIN)
    }
  }
  
  override def readQueryResult(queryId: Long, shouldBroadcast: Boolean = true) = {
    get[AggregatedReadQueryResultResponse](shouldBroadcast) {
      webResource.path(s"/shrine/queries/$queryId/results")
    }
  } 

  private type WebResourceLike = RequestBuilder[WebResource#Builder] with UniformInterface

  //TODO: it would be nice to be able to test post(), get(), and delete()
  private def post[T: Deserializer](shouldBroadcast: Boolean)(webResource: => WebResourceLike): T = perform[T](shouldBroadcast)(webResource, _.post(classOf[String]))

  private def get[T: Deserializer](shouldBroadcast: Boolean)(webResource: => WebResourceLike): T = perform[T](shouldBroadcast)(webResource, _.get(classOf[String]))

  private def delete[T: Deserializer](shouldBroadcast: Boolean)(webResource: => WebResourceLike): T = perform[T](shouldBroadcast)(webResource, _.delete(classOf[String]))

  private[client] def perform[T: Deserializer](shouldBroadcast: Boolean)(webResource: WebResourceLike, httpVerb: UniformInterface => String): T = {

    val withNeededHeaders = webResource.header("Authorization", authorization.toHeader).header("projectId", projectId).header("shouldBroadcast", shouldBroadcast)

    debug(s"Invoking '${webResource.toString}'")
    
    val rawResponse = httpVerb(withNeededHeaders)
    
    debug(s"Got raw response from '${webResource.toString}': '$rawResponse'")
    
    val xml = XML.loadString(rawResponse)

    val deserialize = implicitly[Deserializer[T]]

    try {
      deserialize(breakdownTypes)(xml).get
    } catch {
      case NonFatal(e) => {
        error(s"Error unmarshalling response: $xml", e)
        
        throw e
      }
    }
  }
}

object JerseyShrineClient {
  private[client] type Deserializer[T] = Set[ResultOutputType] => NodeSeq => Try[T]

  private[client] object Deserializer {
    private def toDeserializer[T](f: NodeSeq => T)(implicit discriminator: Int = 42): Deserializer[T] = _ => xml => Try(f(xml))
    
    private def toDeserializer[T](f: Set[ResultOutputType] => NodeSeq => T): Deserializer[T] = breakdownTypes => xml => Try(f(breakdownTypes)(xml))
        
    //TODO: Avoid fragile .get
    private[client] implicit val unFlagQueryResponse: Deserializer[UnFlagQueryResponse] = _ => xml => UnFlagQueryResponse.fromXml(xml)
    
    //TODO: Avoid fragile .get
    private[client] implicit val flagQueryResponse: Deserializer[FlagQueryResponse] = _ => xml => FlagQueryResponse.fromXml(xml)
    
    private[client] implicit val aggregatedReadTranslatedQueryDefinitionResponse: Deserializer[AggregatedReadTranslatedQueryDefinitionResponse] = _ => xml => AggregatedReadTranslatedQueryDefinitionResponse.fromXml(xml)
    
    private[client] implicit val aggregatedReadQueryResultResponseDeserializer: Deserializer[AggregatedReadQueryResultResponse] = toDeserializer(AggregatedReadQueryResultResponse.fromXml _)
    
    private[client] implicit val aggregatedRunQueryResponseDeserializer: Deserializer[AggregatedRunQueryResponse] = AggregatedRunQueryResponse.fromXml _

    private[client] implicit val readPreviousQueriesResponseDeserializer: Deserializer[ReadPreviousQueriesResponse] = toDeserializer(ReadPreviousQueriesResponse.fromXml _)

    private[client] implicit val readApprovedQueryTopicsResponseDeserializer: Deserializer[ReadApprovedQueryTopicsResponse] = toDeserializer(ReadApprovedQueryTopicsResponse.fromXml _)

    private[client] implicit val readQueryInstancesResponseDeserializer: Deserializer[ReadQueryInstancesResponse] = toDeserializer(ReadQueryInstancesResponse.fromXml _)

    private[client] implicit val aggregatedReadInstanceResultsResponseDeserializer: Deserializer[AggregatedReadInstanceResultsResponse] = toDeserializer(AggregatedReadInstanceResultsResponse.fromXml _)

    private[client] implicit val readPdoResponseDeserializer: Deserializer[ReadPdoResponse] = toDeserializer(ReadPdoResponse.fromXml _)

    private[client] implicit val readQueryDefinitionResponseDeserializer: Deserializer[ReadQueryDefinitionResponse] = toDeserializer(ReadQueryDefinitionResponse.fromXml _)

    private[client] implicit val deleteQueryResponseDeserializer: Deserializer[DeleteQueryResponse] = toDeserializer(DeleteQueryResponse.fromXml _)

    private[client] implicit val renameQueryResponseDeserializer: Deserializer[RenameQueryResponse] = toDeserializer(RenameQueryResponse.fromXml _)
  }
}
