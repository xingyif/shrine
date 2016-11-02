package net.shrine.config

/**
 * @author clint
 * @since Jan 17, 2014
 *
 * Keys for Shrine
 */
//todo distribute to where they are used once the rest of config is cleaned up
object Keys {
  val crcEndpoint = "crcEndpoint"
  val shrineSteward = "shrineSteward"
  val ontProjectId = "ontProjectId"
  val crcProjectId = "crcProjectId"
  val setSizeObfuscation = "setSizeObfuscation"
  val isAdapter = "isAdapter"
  val isBroadcaster = "isBroadcaster"
  val adapterLockoutAttemptsThreshold = "adapterLockoutAttemptsThreshold"
  val adapterMappingsFileName = "adapterMappingsFileName"
  val adapterMappingsFileType = "adapterMappingsFileType"
  val downstreamNodes = "downstreamNodes"
  val maxSignatureAge = "maxSignatureAge"
  val adapter = "adapter"
  val hub = "hub"
  val queryEntryPoint = "queryEntryPoint" //todo remove once it's not used anymore
  val broadcasterIsLocal = "broadcasterIsLocal"
  val broadcasterServiceEndpoint = "broadcasterServiceEndpoint"
  val immediatelyRunIncomingQueries = "immediatelyRunIncomingQueries"
  val authenticationType = "authenticationType"
}
