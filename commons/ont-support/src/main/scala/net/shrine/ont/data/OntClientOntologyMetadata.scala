package net.shrine.ont.data

import net.shrine.client.OntClient

/**
 * @author clint
 * @since Jan 28, 2014
 */
final case class OntClientOntologyMetadata(client: OntClient) {
  def ontologyVersion: String = {
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