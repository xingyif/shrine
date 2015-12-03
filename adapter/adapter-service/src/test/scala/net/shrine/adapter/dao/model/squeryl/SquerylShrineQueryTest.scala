package net.shrine.adapter.dao.model.squeryl

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.query.Term
import net.shrine.util.XmlDateHelper
import net.shrine.dao.DateHelpers
import net.shrine.protocol.query.QueryDefinition

/**
 * @author clint
 * @date Jan 26, 2015
 */
final class SquerylShrineQueryTest extends ShouldMatchersForJUnit {
  private val expr = Term("foo")
  private val domain = "some-domain"
  private val user = "some-user"
  private val queryName = "some-query"
  private val networkQueryId = 12345L
  private val localId = "local-id"
  private val id = 1
  private val dateCreated = XmlDateHelper.now
  private val isFlagged = true
  private val flagMessage = Some("flag-message")
  private val hasBeenRun = true

  @Test
  def testToShrineQueryNullQueryDefNonNullQueryExpr: Unit = {

    val squerylShrineQuery = SquerylShrineQuery(
      id,
      localId,
      networkQueryId,
      queryName,
      user,
      domain,
      Some(expr.toXmlString),
      DateHelpers.toTimestamp(dateCreated),
      isFlagged,
      flagMessage,
      hasBeenRun,
      queryXml = None)

    val shrineQuery = squerylShrineQuery.toShrineQuery

    shrineQuery.dateCreated should equal(dateCreated)
    shrineQuery.domain should equal(domain)
    shrineQuery.username should equal(user)
    shrineQuery.flagMessage should equal(flagMessage)
    shrineQuery.hasBeenRun should equal(hasBeenRun)
    shrineQuery.hasNotBeenRun should equal(!hasBeenRun)
    shrineQuery.id should equal(id)
    shrineQuery.isFlagged should equal(isFlagged)
    shrineQuery.localId should equal(localId)
    shrineQuery.name should equal(queryName)
    shrineQuery.networkId should equal(networkQueryId)
    shrineQuery.queryDefinition should equal(QueryDefinition(queryName, expr))
  }
  
  @Test
  def testToShrineQueryNonNullQueryDefNullQueryExpr: Unit = {

    val queryDef = QueryDefinition(queryName, expr)
    
    val squerylShrineQuery = SquerylShrineQuery(
      id,
      localId,
      networkQueryId,
      queryName,
      user,
      domain,
      queryExpr = None,
      DateHelpers.toTimestamp(dateCreated),
      isFlagged,
      flagMessage,
      hasBeenRun,
      Some(queryDef.toXmlString))

    val shrineQuery = squerylShrineQuery.toShrineQuery

    shrineQuery.dateCreated should equal(dateCreated)
    shrineQuery.domain should equal(domain)
    shrineQuery.username should equal(user)
    shrineQuery.flagMessage should equal(flagMessage)
    shrineQuery.hasBeenRun should equal(hasBeenRun)
    shrineQuery.hasNotBeenRun should equal(!hasBeenRun)
    shrineQuery.id should equal(id)
    shrineQuery.isFlagged should equal(isFlagged)
    shrineQuery.localId should equal(localId)
    shrineQuery.name should equal(queryName)
    shrineQuery.networkId should equal(networkQueryId)
    shrineQuery.queryDefinition should equal(queryDef)
  }
  
  @Test
  def testToShrineQueryNullQueryDefNullQueryExpr: Unit = {

    val squerylShrineQuery = SquerylShrineQuery(
      id,
      localId,
      networkQueryId,
      queryName,
      user,
      domain,
      queryExpr = null,
      DateHelpers.toTimestamp(dateCreated),
      isFlagged,
      flagMessage,
      hasBeenRun,
      queryXml = null)

    intercept[Exception] {
      squerylShrineQuery.toShrineQuery
    }
  }
}