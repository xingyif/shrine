package net.shrine.utilities.batchquerier.components

import net.shrine.utilities.batchquerier.HasTrustParam
import net.shrine.crypto.TrustParam
import TrustParam.AcceptAllCerts

/**
 * @author clint
 * @date Nov 22, 2013
 */
trait TrustsAllCerts extends HasTrustParam {
  override val trustParam: TrustParam = AcceptAllCerts
}