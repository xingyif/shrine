package net.shrine.crypto2

/**
  * Created by ty on 10/25/16.
  */
case class DownStreamCertCollection(override val myEntry: KeyStoreEntry, caEntry: KeyStoreEntry, hubSite: RemoteSite)
  extends AbstractHubCertCollection(myEntry, caEntry)
{
  override val remoteSites = hubSite +: Nil
}