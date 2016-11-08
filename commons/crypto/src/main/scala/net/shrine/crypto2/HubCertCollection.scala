package net.shrine.crypto2

/**
  * Created by ty on 11/4/16.
  */
case class HubCertCollection(override val myEntry: KeyStoreEntry, caEntry: KeyStoreEntry, override val remoteSites: Seq[RemoteSite])
  extends AbstractHubCertCollection(myEntry, caEntry)
