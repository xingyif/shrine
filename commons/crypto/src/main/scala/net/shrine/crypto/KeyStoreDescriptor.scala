package net.shrine.crypto

/**
 * @author clint
 * @since Nov 22, 2013
 */

//todo consolidate with KeyStoreParser, maybe combine the whole works into KeyStoreCertCollection's collection
final case class KeyStoreDescriptor(
    file: String, 
    password: String, 
    privateKeyAlias: Option[String], 
    caCertAliases: Seq[String], 
    keyStoreType: KeyStoreType = KeyStoreType.Default)