package net.shrine.adapter.dao.model

import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.protocol.query.{QueryDefinition, Term}
import net.shrine.util.XmlDateHelper
import net.shrine.protocol.QueryMaster

/**
 * @author clint
 * @date Oct 31, 2012
 */
final class ShrineQueryTest extends ShouldMatchersForJUnit {
  @Test
  def testToQueryMaster {
    val message = "askljdkaljsd"

    val queryName = "some-query-name"
    val queryExpr = Term("nuh")
    val queryDefinition = QueryDefinition(queryName,queryExpr)
    val shrineQuery = ShrineQuery(123, "master-id", 456L, "some-query-name", "foo", "bar", XmlDateHelper.now, isFlagged = true, flagMessage = Some(message),queryDefinition)

    def doTestToQueryMaster(idField: ShrineQuery => String) {
      val queryMaster = shrineQuery.toQueryMaster(idField)

      //TODO: Should this be the real i2b2 master id now?  That would break previous queries, though
      queryMaster.queryMasterId should equal(idField(shrineQuery))
      queryMaster.networkQueryId should equal(shrineQuery.networkId)
      queryMaster.name should equal(shrineQuery.name)
      queryMaster.userId should equal(shrineQuery.username)
      queryMaster.groupId should equal(shrineQuery.domain)
      queryMaster.createDate should equal(shrineQuery.dateCreated)
      queryMaster.flagged should equal(Some(true))
      queryMaster.flagMessage should equal(Some(message))
      
      shrineQuery.copy(isFlagged = false).toQueryMaster(idField).flagged should equal(Some(false))
      shrineQuery.copy(isFlagged = false).toQueryMaster(idField).flagMessage should equal(None)
    }

    doTestToQueryMaster(_.networkId.toString)
    
    doTestToQueryMaster(_.localId)
  }
}