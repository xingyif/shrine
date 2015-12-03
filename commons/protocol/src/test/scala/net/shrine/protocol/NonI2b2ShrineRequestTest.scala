package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term

/**
 * @author clint
 * @date Feb 13, 2014
 */
final class NonI2b2ShrineRequestTest extends ShouldMatchersForJUnit {
  @Test
  def testFromXml {
    import scala.concurrent.duration._

    val authn = AuthenticationInfo("d", "u", Credential("p", false))
    val waitTime = 1.minute
    val projectId = "askljdal"

    import NonI2b2ShrineRequest.fromXml

    fromXml(DefaultBreakdownResultOutputTypes.toSet)(<foo/>).isFailure should be(true)

    def doRoundTrip(req: NonI2b2ShrineRequest): Unit = {
      fromXml(DefaultBreakdownResultOutputTypes.toSet)(req.toXml).get should equal(req)
    }

    doRoundTrip(ReadTranslatedQueryDefinitionRequest(authn, waitTime, QueryDefinition("foo", Term("ksjhksdajfh"))))

    doRoundTrip(ReadQueryResultRequest(projectId, waitTime, authn, 12345L))
  }
}