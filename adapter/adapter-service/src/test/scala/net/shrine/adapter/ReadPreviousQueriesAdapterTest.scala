package net.shrine.adapter

import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit

import net.shrine.adapter.dao.squeryl.AbstractSquerylAdapterTest
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.Credential
import net.shrine.protocol.ReadPreviousQueriesRequest
import net.shrine.protocol.ReadPreviousQueriesResponse
import net.shrine.protocol.query.{QueryDefinition, Term}

/**
 * @author clint
 * @since Oct 30, 2012
 */
final class ReadPreviousQueriesAdapterTest extends AbstractSquerylAdapterTest with ShouldMatchersForJUnit {
  @Test
  def testProcessRequest = afterCreatingTables {
    val Seq((masterId1, queryId1, authn1, queryD1), (masterId2, queryId2, authn2, queryD2)) = (1 to 2).map(i => (s"masterid:$i", i, AuthenticationInfo("some-domain", s"user$i", Credential("salkhfkjas", false)), QueryDefinition(s"query$i",Term(i.toString))))
    
    val masterId3 = "kalsjdklasdjklasdlkjaldsagtuegthasgf"
    val queryId3 = queryId1 + 42
    
    //2 queries for authn1, 1 for authn2
    dao.insertQuery(masterId1, queryId1, authn1, queryD1, isFlagged = false, hasBeenRun = true, flagMessage = None)
    
    //Make next query happen 10 milliseconds later, so we can distinguish it from the one we just inserted 
    //(java.sql.Timestamps have 1ms resolution, it appears?)
    Thread.sleep(10)
    
    dao.insertQuery(masterId2, queryId2, authn2, queryD2, isFlagged = false, hasBeenRun = true, flagMessage = None)
    
    //Make next query happen 10 milliseconds later, so we can distinguish it from the one we just inserted 
    //(java.sql.Timestamps have 1ms resolution, it appears?)
    Thread.sleep(10)
    
    dao.insertQuery(masterId3, queryId3, authn1, queryD1, isFlagged = false, hasBeenRun = true, flagMessage = None)
    
    val adapter = new ReadPreviousQueriesAdapter(dao)
    
    def processRequest(authn: AuthenticationInfo, req: ReadPreviousQueriesRequest) = adapter.processRequest(BroadcastMessage(authn, req)).asInstanceOf[ReadPreviousQueriesResponse]

    import scala.concurrent.duration._
    
    {
      //bogus id
      val bogusDomain = "alskdjlasd"
      val bogusUser = "salkjdlas"

      val bogusAuthn = AuthenticationInfo(bogusDomain, bogusUser, Credential("sadasdsad", false))
      
      val req = ReadPreviousQueriesRequest("some-projectId", 1.second, bogusAuthn, bogusUser, 5)
        
      val result = processRequest(bogusAuthn, req)
      
      result.queryMasters should equal(Nil)
    }
    
    //Should get 2 QueryMasters for authn1
    {
      val req = ReadPreviousQueriesRequest("some-projectId", 1.second, authn1, authn1.username, 5)
      
      val result = processRequest(authn1, req)
      
      val Seq(queryMaster1, queryMaster2) = result.queryMasters.sortBy(_.queryMasterId)
      
      queryMaster1.queryMasterId should equal(queryId1.toString)
      queryMaster1.name should equal(queryD1.name)
      queryMaster1.userId should equal(authn1.username)
      queryMaster1.groupId should equal(authn1.domain)
      queryMaster1.createDate should not be(null) // :/
      
      queryMaster2.queryMasterId should equal(queryId3.toString)
      queryMaster2.name should equal(queryD1.name)
      queryMaster2.userId should equal(authn1.username)
      queryMaster2.groupId should equal(authn1.domain)
      queryMaster2.createDate should not be(null) // :/
    }
    
    //Should get 1 QueryMaster for authn2
    {
      val req = ReadPreviousQueriesRequest("some-projectId", 1.second, authn1, authn1.username, 5)
      
      val result = processRequest(authn2, req)
      
      val Seq(queryMaster) = result.queryMasters
      
      queryMaster.queryMasterId should equal(queryId2.toString)
      queryMaster.name should equal(queryD2.name)
      queryMaster.userId should equal(authn2.username)
      queryMaster.groupId should equal(authn2.domain)
      queryMaster.createDate should not be(null) // :/
    }
    
    //Limit to fewer prev. queries than are in the DB
    {
      val req = ReadPreviousQueriesRequest("some-projectId", 1.second, authn1, authn1.username, 1)
      
      val result = processRequest(authn1, req)
      
      val Seq(queryMaster1) = result.queryMasters.sortBy(_.queryMasterId)
      
      queryMaster1.queryMasterId should equal(queryId3.toString)
      queryMaster1.name should equal(queryD1.name)
      queryMaster1.userId should equal(authn1.username)
      queryMaster1.groupId should equal(authn1.domain)
      queryMaster1.createDate should not be(null) // :/
    }
  }
}