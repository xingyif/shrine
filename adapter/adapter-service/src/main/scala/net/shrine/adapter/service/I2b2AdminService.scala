package net.shrine.adapter.service

import net.shrine.adapter.dao.AdapterDao
import net.shrine.adapter.dao.I2b2AdminDao
import net.shrine.client.HttpClient
import net.shrine.i2b2.protocol.pm.User
import net.shrine.log.Loggable
import net.shrine.protocol.I2b2AdminRequestHandler
import net.shrine.protocol.ReadI2b2AdminPreviousQueriesRequest
import net.shrine.protocol.ReadQueryDefinitionRequest
import net.shrine.protocol.ShrineRequest
import net.shrine.protocol.ShrineResponse
import net.shrine.client.Poster
import net.shrine.protocol.ReadI2b2AdminQueryingUsersRequest
import net.shrine.protocol.I2b2AdminReadQueryDefinitionRequest
import net.shrine.authorization.PmAuthorizerComponent
import net.shrine.authorization.PmHttpClientComponent
import net.shrine.protocol.RunHeldQueryRequest
import net.shrine.adapter.RunQueryAdapter
import net.shrine.adapter.components.HeldQueries
import net.shrine.adapter.components.I2b2AdminPreviousQueries
import net.shrine.adapter.components.I2b2AdminQueryingUsers
import net.shrine.adapter.components.QueryDefinitions
import net.shrine.authorization.PmAuthorizerComponent
import net.shrine.authorization.PmHttpClientComponent



/**
 * @author clint
 * @date Apr 4, 2013
 * 
 * Exposes an http API dictated to us by the i2b2 team for use by the i2b2 workbench app. 
 * Details of the implementation of the API were mandated by people at the PI level. Sigh.
 */
final class I2b2AdminService(
    dao: AdapterDao,
	i2b2AdminDao: I2b2AdminDao,
	override val pmPoster: Poster,
	runQueryAdapter: RunQueryAdapter) extends 
		I2b2AdminRequestHandler with 
		PmAuthorizerComponent with 
		PmHttpClientComponent with 
		Loggable {
  
  require(dao != null)
  require(i2b2AdminDao != null)
  require(pmPoster != null)
  
  info("I2b2 admin service initialized")
  
  private lazy val heldQueries = HeldQueries(dao, runQueryAdapter)
  
  private lazy val i2b2AdminPreviousQueries = I2b2AdminPreviousQueries(i2b2AdminDao)
  
  private lazy val i2b2AdminQueryingUsers = I2b2AdminQueryingUsers(i2b2AdminDao)
  
  private lazy val queryDefinitions = QueryDefinitions[I2b2AdminReadQueryDefinitionRequest](dao)
  
  //NB: shouldBroadcast is ignored; we never broadcast
  override def readQueryDefinition(request: I2b2AdminReadQueryDefinitionRequest, shouldBroadcast: Boolean): ShrineResponse = {
    checkWithPmAndThen(request) {
      queryDefinitions.get
    }
  }

  //NB: shouldBroadcast is ignored; we never broadcast
  override def readI2b2AdminPreviousQueries(request: ReadI2b2AdminPreviousQueriesRequest, shouldBroadcast: Boolean): ShrineResponse = {
    checkWithPmAndThen(request) {
      i2b2AdminPreviousQueries.get
    }
  }
  
  //NB: shouldBroadcast is ignored; we never broadcast
  override def readI2b2AdminQueryingUsers(request: ReadI2b2AdminQueryingUsersRequest, shouldBroadcast: Boolean): ShrineResponse = {
    checkWithPmAndThen(request) {
      i2b2AdminQueryingUsers.get
    }
  }
  
  //NB: shouldBroadcast is ignored; we never broadcast
  override def runHeldQuery(request: RunHeldQueryRequest, shouldBroadcast: Boolean): ShrineResponse = {
    checkWithPmAndThen(request) {
      heldQueries.run
    }
  }

  def checkWithPmAndThen[Req <: ShrineRequest](request: Req)(f: Req => ShrineResponse): ShrineResponse = {
    import PmAuthorizerComponent._

    val authorized = Pm.authorize(request.projectId, Set(User.Roles.Manager), request.authn) 
    
    authorized match {
      case Authorized(user) => f(request) 
      case na: NotAuthorized => na.toErrorResponse
    }
  }
}
