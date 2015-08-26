package net.shrine.adapter

import net.shrine.protocol.HiveCredentials

/**
 * @author clint
 * @date Nov 8, 2012
 */
abstract class WithHiveCredentialsAdapter(protected val hiveCredentials: HiveCredentials) extends Adapter