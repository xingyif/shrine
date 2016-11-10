package net.shrine.utilities.batchquerier.components

import net.shrine.crypto2.TrustParam
import net.shrine.crypto2.TrustParam.AcceptAllCerts
import net.shrine.utilities.batchquerier.HasTrustParam

/**
 * @author clint
 * @date Nov 22, 2013
 */
trait TrustsAllCerts extends HasTrustParam {
  override val trustParam: TrustParam = AcceptAllCerts
}