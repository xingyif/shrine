package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Oct 3, 2014
 */
final class CrcRequestTypeTest extends ShouldMatchersForJUnit {
  @Test
  def testWithI2b2RequestType: Unit = {
    
    import CrcRequestType.withI2b2RequestType
    
    for {
      crcReqType <- CrcRequestType.values
    } {
      withI2b2RequestType(crcReqType.i2b2RequestType) should equal(Some(crcReqType))
    }
    
    withI2b2RequestType(null) should be(None)
    withI2b2RequestType("sadjkhkasd") should be(None)
  }
}