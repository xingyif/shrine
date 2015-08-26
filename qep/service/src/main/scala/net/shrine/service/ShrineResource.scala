package net.shrine.service

import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import net.shrine.log.Loggable
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.DeleteQueryRequest
import net.shrine.protocol.OutputTypeSet
import net.shrine.protocol.ReadApprovedQueryTopicsRequest
import net.shrine.protocol.ReadInstanceResultsRequest
import net.shrine.protocol.ReadPdoRequest
import net.shrine.protocol.ReadPreviousQueriesRequest
import net.shrine.protocol.ReadQueryDefinitionRequest
import net.shrine.protocol.ReadQueryInstancesRequest
import net.shrine.protocol.ReadQueryResultRequest
import net.shrine.protocol.RenameQueryRequest
import net.shrine.protocol.RunQueryRequest
import net.shrine.protocol.ShrineRequestHandler
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.query.QueryDefinition
import scala.xml.XML
import net.shrine.protocol.BaseShrineResponse
import net.shrine.protocol.ReadTranslatedQueryDefinitionRequest
import net.shrine.protocol.FlagQueryRequest
import net.shrine.protocol.UnFlagQueryRequest

/**
 * @author Bill Simons
 * @author Clint Gilbert
 * @date 8/30/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
@Path("/shrine")
@Produces(Array(MediaType.APPLICATION_XML)) //NB: Is a case class to get apply on the companion object, for smoother testing
final case class ShrineResource(shrineRequestHandler: ShrineRequestHandler) extends Loggable {
  import ShrineResource.waitTime

  @Consumes(Array(MediaType.TEXT_PLAIN))
  @POST
  @Path("/queries/{queryId}/flag")
  def flagQuery(
    @HeaderParam("projectId") projectId: String,
    //authorization will be constructed by JAXRS using the String value of the 'Authorization' header
    @HeaderParam("Authorization") authorization: AuthenticationInfo,
    @PathParam("queryId") networkQueryId: Long,
    @HeaderParam("shouldBroadcast") shouldBroadcast: Boolean,
    flagMessage: String): String = {
    
    val flagMessageOption = Option(flagMessage).filter(!_.trim.isEmpty)
    
    //TODO: What should we return, if anything?
    performAndSerialize(_.flagQuery(FlagQueryRequest(projectId, waitTime, authorization, networkQueryId, flagMessageOption), shouldBroadcast))
  }
  
  @POST
  @Path("/queries/{queryId}/unflag")
  def unFlagQuery(
    @HeaderParam("projectId") projectId: String,
    //authorization will be constructed by JAXRS using the String value of the 'Authorization' header
    @HeaderParam("Authorization") authorization: AuthenticationInfo,
    @PathParam("queryId") networkQueryId: Long,
    @HeaderParam("shouldBroadcast") shouldBroadcast: Boolean): String = {
    
    //TODO: What should we return, if anything?
    performAndSerialize(_.unFlagQuery(UnFlagQueryRequest(projectId, waitTime, authorization, networkQueryId), shouldBroadcast))
  }

  @GET
  @Path("{userId}/approved-topics")
  def readApprovedQueryTopics(
    @HeaderParam("projectId") projectId: String,
    //authorization will be constructed by JAXRS using the String value of the 'Authorization' header
    @HeaderParam("Authorization") authorization: AuthenticationInfo,
    @PathParam("userId") userId: String,
    @HeaderParam("shouldBroadcast") shouldBroadcast: Boolean): String = {

    performAndSerialize(_.readApprovedQueryTopics(ReadApprovedQueryTopicsRequest(projectId, waitTime, authorization, userId), shouldBroadcast))
  }

  @GET
  @Path("{userId}/queries")
  def readPreviousQueries(
    @HeaderParam("projectId") projectId: String,
    //authorization will be constructed by JAXRS using the String value of the 'Authorization' header
    @HeaderParam("Authorization") authorization: AuthenticationInfo,
    @PathParam("userId") userId: String,
    @QueryParam("fetchSize") fetchSize: Int,
    @HeaderParam("shouldBroadcast") shouldBroadcast: Boolean): Response = {

    if (userId != authorization.username) {
      Response.status(403).build
    } else {
      val fSize = if (fetchSize != 0) fetchSize else 20

      Response.ok.entity {
        performAndSerialize(_.readPreviousQueries(ReadPreviousQueriesRequest(projectId, waitTime, authorization, userId, fSize), shouldBroadcast))
      }.build
    }
  }

  @POST
  @Path("/queries")
  @Consumes(Array(MediaType.APPLICATION_XML))
  def runQuery(
    @HeaderParam("projectId") projectId: String,
    //authorization will be constructed by JAXRS using the String value of the 'Authorization' header
    @HeaderParam("Authorization") authorization: AuthenticationInfo,
    @HeaderParam("topicId") topicId: String,
    //outputTypes will be constructed by JAXRS using the String value of the 'outputTypes' header
    @HeaderParam("outputTypes") outputTypes: OutputTypeSet,
    queryDefinitionXml: String,
    @HeaderParam("shouldBroadcast") shouldBroadcast: Boolean): String = {

    val queryDef = QueryDefinition.fromXml(queryDefinitionXml).get

    val topicIdOption = Option(topicId).filter(!_.trim.isEmpty)

    debug(s"runQuery() with $shrineRequestHandler and $queryDef")

    //NB: Create the RunQueryRequest with a dummy networkQueryId of '-1'; 
    //this will be filled in with an appropriately-generated value by the ShrineRequestHandler
    performAndSerialize(_.runQuery(RunQueryRequest(projectId, waitTime, authorization, -1, topicIdOption, outputTypes.toSet, queryDef), shouldBroadcast))
  }

  @GET
  @Path("/queries/{queryId}/instances")
  def readQueryInstances(
    @HeaderParam("projectId") projectId: String,
    //authorization will be constructed by JAXRS using the String value of the 'Authorization' header
    @HeaderParam("Authorization") authorization: AuthenticationInfo,
    @PathParam("queryId") queryId: Long,
    @HeaderParam("shouldBroadcast") shouldBroadcast: Boolean): String = {

    performAndSerialize(_.readQueryInstances(ReadQueryInstancesRequest(projectId, waitTime, authorization, queryId), shouldBroadcast))
  }

  @GET
  @Path("/instances/{instanceId}/results")
  def readInstanceResults(
    @HeaderParam("projectId") projectId: String,
    //authorization will be constructed by JAXRS using the String value of the 'Authorization' header
    @HeaderParam("Authorization") authorization: AuthenticationInfo,
    @PathParam("instanceId") instanceId: Long,
    @HeaderParam("shouldBroadcast") shouldBroadcast: Boolean): String = {

    performAndSerialize(_.readInstanceResults(ReadInstanceResultsRequest(projectId, waitTime, authorization, instanceId), shouldBroadcast))
  }

  @POST //This must be POST, since we're sending content in the request body
  @Path("/patient-set/{patientSetCollId}")
  @Consumes(Array(MediaType.APPLICATION_XML))
  def readPdo(
    @HeaderParam("projectId") projectId: String,
    //authorization will be constructed by JAXRS using the String value of the 'Authorization' header
    @HeaderParam("Authorization") authorization: AuthenticationInfo,
    @PathParam("patientSetCollId") patientSetCollId: String,
    optionsXml: String,
    @HeaderParam("shouldBroadcast") shouldBroadcast: Boolean): String = {

    import XML.loadString

    performAndSerialize(_.readPdo(ReadPdoRequest(projectId, waitTime, authorization, patientSetCollId, loadString(optionsXml)), shouldBroadcast))
  }

  @GET
  @Path("/queries/{queryId}")
  def readQueryDefinition(
    @HeaderParam("projectId") projectId: String,
    //authorization will be constructed by JAXRS using the String value of the 'Authorization' header
    @HeaderParam("Authorization") authorization: AuthenticationInfo,
    @PathParam("queryId") queryId: Long,
    @HeaderParam("shouldBroadcast") shouldBroadcast: Boolean): String = {

    performAndSerialize(_.readQueryDefinition(ReadQueryDefinitionRequest(projectId, waitTime, authorization, queryId), shouldBroadcast))
  }

  @DELETE
  @Path("/queries/{queryId}")
  def deleteQuery(
    @HeaderParam("projectId") projectId: String,
    //authorization will be constructed by JAXRS using the String value of the 'Authorization' header
    @HeaderParam("Authorization") authorization: AuthenticationInfo,
    @PathParam("queryId") queryId: Long,
    @HeaderParam("shouldBroadcast") shouldBroadcast: Boolean): String = {

    performAndSerialize(_.deleteQuery(DeleteQueryRequest(projectId, waitTime, authorization, queryId), shouldBroadcast))
  }

  @POST
  @Path("/queries/{queryId}/name")
  @Consumes(Array(MediaType.TEXT_PLAIN))
  def renameQuery(
    @HeaderParam("projectId") projectId: String,
    //authorization will be constructed by JAXRS using the String value of the 'Authorization' header
    @HeaderParam("Authorization") authorization: AuthenticationInfo,
    @PathParam("queryId") queryId: Long,
    queryName: String,
    @HeaderParam("shouldBroadcast") shouldBroadcast: Boolean): String = {

    performAndSerialize(_.renameQuery(RenameQueryRequest(projectId, waitTime, authorization, queryId, queryName), shouldBroadcast))
  }

  @GET
  @Path("/queries/{queryId}/results")
  @Consumes(Array(MediaType.TEXT_PLAIN))
  def readQueryResults(
    @HeaderParam("projectId") projectId: String,
    //authorization will be constructed by JAXRS using the String value of the 'Authorization' header
    @HeaderParam("Authorization") authorization: AuthenticationInfo,
    @PathParam("queryId") queryId: Long,
    @HeaderParam("shouldBroadcast") shouldBroadcast: Boolean): String = {

    performAndSerialize(_.readQueryResult(ReadQueryResultRequest(projectId, waitTime, authorization, queryId), shouldBroadcast))
  }

  @POST
  @Path("/queries/translated")
  @Consumes(Array(MediaType.APPLICATION_XML))
  def readTranslatedQueryDefinition(
    @HeaderParam("projectId") projectId: String,
    //authorization will be constructed by JAXRS using the String value of the 'Authorization' header
    @HeaderParam("Authorization") authorization: AuthenticationInfo,
    @HeaderParam("shouldBroadcast") shouldBroadcast: Boolean,
    queryDefinitionXml: String): String = {

    val queryDef = QueryDefinition.fromXml(queryDefinitionXml).get

    //NB: Create the RunQueryRequest with a dummy networkQueryId of '-1'; 
    //this will be filled in with an appropriately-generated value by the ShrineRequestHandler
    performAndSerialize(_.readTranslatedQueryDefinition(ReadTranslatedQueryDefinitionRequest(authorization, waitTime, queryDef), shouldBroadcast))
  }

  private def performAndSerialize[R <: BaseShrineResponse](op: ShrineRequestHandler => R): String = {
    op(shrineRequestHandler).toXmlString
  }
}

//NB: extends ShrineRequestHandler => ShrineResource for smoother testing syntax
object ShrineResource extends (ShrineRequestHandler => ShrineResource) {
  import scala.concurrent.duration._

  val waitTime = 10.seconds
}

