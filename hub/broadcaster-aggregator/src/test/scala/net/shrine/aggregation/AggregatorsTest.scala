package net.shrine.aggregation

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.protocol.RunQueryRequest

/**
 * @author clint
 * @since Mar 14, 2013
 */
final class AggregatorsTest extends ShouldMatchersForJUnit {
  @Test
  def testForRunQueryRequest() {

    val authn = AuthenticationInfo("some-domain", "some-user", Credential("some-password", isToken = false))
    val projectId = "projectId"
    val queryDef = QueryDefinition("yo", Term("foo"))
    
    import scala.concurrent.duration._
    
    val request = RunQueryRequest(projectId, 1.millisecond, authn, 0L, Some(("topicId","Topic Name")), Set.empty, queryDef)
    
    def doTestRunQueryAggregatorFor(addAggregatedResult: Boolean) {
      val aggregator = Aggregators.forRunQueryRequest(addAggregatedResult)(request)

      aggregator should not be null

      aggregator.queryId should be(-1L)
      aggregator.groupId should be(projectId)
      aggregator.userId should be(authn.username)
      aggregator.requestQueryDefinition should be(queryDef)
      aggregator.addAggregatedResult should be(addAggregatedResult)
    }

    doTestRunQueryAggregatorFor(true)
    doTestRunQueryAggregatorFor(false)
  }
}