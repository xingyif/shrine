package net.shrine.protocol

import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author clint
 * @date Oct 3, 2014
 */
final class RequestTypeTest extends ShouldMatchersForJUnit {
  @Test
  def testWithCrcRequestType: Unit = {
    import RequestType.withCrcRequestType
    
    for {
      reqType <- RequestType.values
      crcReqType <- reqType.crcRequestType
    } {
      withCrcRequestType(crcReqType) should be(Some(reqType))
    }
    
    withCrcRequestType(null) should be(None)
  }
}