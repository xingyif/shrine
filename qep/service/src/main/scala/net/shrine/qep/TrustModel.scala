package net.shrine.qep

/**
  * Created by ty on 10/14/16.
  *
  * Represents what SHRINE thinks the current network architecture is,
  * i.e., a Hub or P2P model. For now this just gets read from the config,
  * however, going forward we can do better: <a href=https://open.med.harvard.edu/jira/browse/SHRINE-1729>Jira Ticket</a>
  */
sealed trait TrustModel {
  val description:String = this match {
    case SingleHubModel => "Central Certificate Authority"
    case PeerToPeerModel => "Peer-to-Peer Certificate Authority"
  }
}

case object SingleHubModel extends TrustModel

case object PeerToPeerModel extends TrustModel
