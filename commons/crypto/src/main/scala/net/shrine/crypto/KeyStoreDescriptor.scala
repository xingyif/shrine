package net.shrine.crypto

import java.io.File

/**
 * @author clint
 * @date Nov 22, 2013
 */
final case class KeyStoreDescriptor(
    file: String, 
    password: String, 
    privateKeyAlias: Option[String], 
    caCertAliases: Seq[String], 
    keyStoreType: KeyStoreType = KeyStoreType.Default)