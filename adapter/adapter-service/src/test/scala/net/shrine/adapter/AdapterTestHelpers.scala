package net.shrine.adapter

import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.QueryMaster
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.util.XmlDateHelper

/**
 * @author clint
 * @date Nov 28, 2012
 */
trait AdapterTestHelpers {
  val queryId = 123
  val localMasterId = "kasjdlsajdklajsdkljasd"
  val bogusQueryId = 999
  
  val masterId1 = "1"
  val masterId2 = "2"
  val masterId3 = "3"
  val masterId4 = "4"

  val networkQueryId1 = 1L
  val networkQueryId2 = 2L
  val networkQueryId3 = 3L
  val networkQueryId4 = 4L

  val queryName1 = "query-name1"
  val queryName2 = "query-name2"

  lazy val queryDef1 = QueryDefinition(queryName1, Term("x"))
  lazy val queryDef2 = QueryDefinition(queryName2, Term("y"))

  val userId = "some-other-user"

  val domain = "Some-other-domain"
  val anotherDomain = "some-completely-different-domain"

  val password = "some-val"

  lazy val authn = AuthenticationInfo(domain, userId, Credential(password, false))
  lazy val authn2 = AuthenticationInfo(authn.domain, "a-different-user", Credential("jkafhkjdhsfjksdhfkjsdg", false))
  
  val projectId = "some-project-id"

  import scala.concurrent.duration._
    
  val waitTime = 12345.milliseconds

  lazy val queryMaster1 = QueryMaster(masterId1, networkQueryId1, queryName1, userId, domain, XmlDateHelper.now, held = Some(false), flagged = Some(true))
  lazy val queryMaster2 = QueryMaster(masterId2, networkQueryId2, queryName2, userId, domain, XmlDateHelper.now, held = Some(false), flagged = Some(false))
  lazy val queryMaster4 = QueryMaster(masterId4, networkQueryId4, queryName2, authn2.username, anotherDomain, XmlDateHelper.now, held = Some(false), flagged = Some(true))
}