package net.shrine.crypto

import java.security.PrivateKey
import java.security.PublicKey

/**
 * @author clint
 * @date Nov 25, 2013
 */
final case class KeyPair(publicKey: PublicKey, privateKey: PrivateKey)