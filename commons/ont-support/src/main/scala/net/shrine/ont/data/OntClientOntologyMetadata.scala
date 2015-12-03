package net.shrine.ont.data

import net.shrine.client.OntClient
import net.shrine.client.Poster
import net.shrine.client.JerseyHttpClient
import net.shrine.client.PosterOntClient
import net.shrine.crypto.TrustParam
import net.shrine.protocol.HiveCredentials

/**
 * @author clint
 * @date Jan 28, 2014
 */
final class OntClientOntologyMetadata(client: OntClient) extends OntologyMetadata {
  override def ontologyVersion: String = {
    import OntClientOntologyMetadata._

    val versionTermOption = for {
      versionTerm <- client.childrenOf(versionContainerTerm).headOption
      lastPathPart <- dropTrailingSlash(versionTerm).split(backslashRegex).lastOption
    } yield lastPathPart

    versionTermOption.getOrElse("UNKNOWN")
  }
}

object OntClientOntologyMetadata {
  private val backslash = """\"""
  private val backslashRegex = """\\"""

  def dropTrailingSlash(s: String): String = if (s.endsWith(backslash)) s.dropRight(1) else s

  val versionContainerTerm = """\\SHRINE\SHRINE\ONTOLOGYVERSION"""
}
