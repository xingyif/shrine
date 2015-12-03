package net.shrine.config.mappings

import scala.xml.NodeSeq
import net.shrine.util.XmlUtil
import scala.util.Try
import net.shrine.util.XmlDateHelper
import net.shrine.util.Tries
import java.io.Reader
import scala.io.Source
import au.com.bytecode.opencsv.CSVReader
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer
import java.io.StringWriter
import au.com.bytecode.opencsv.CSVWriter

/**
 * AdapterMappings associate "global/core" shrine terms with local terms.
 * A single global term can have MANY local term mappings.
 *
 * @author Clint Gilbert
 * @since 2013 (?)
 */
final case class AdapterMappings(version: String = AdapterMappings.Unknown, mappings: Map[String, Set[String]] = Map.empty) {

  def networkTerms: Set[String] = mappings.keySet

  def localTermsFor(networkTerm: String): Set[String] = mappings.get(networkTerm).getOrElse(Set.empty)

  def size = mappings.size

  def ++(ms: Iterable[(String, String)]): AdapterMappings = ms.foldLeft(this)(_ + _)

  def ++(ms: Iterator[(String, String)])(implicit discriminator: Int = 42): AdapterMappings = ms.foldLeft(this)(_ + _)

  def +(mapping: (String, String)): AdapterMappings = {
    val (networkTerm, localTerm) = mapping

    mappings.get(networkTerm) match {
      case Some(localTerms) if localTerms.contains(localTerm) => this
      case possiblyExtantMapping => {
        val newLocalTerms = possiblyExtantMapping.getOrElse(Set.empty) + localTerm

        copy(mappings = mappings + (networkTerm -> newLocalTerms))
      }
    }
  }

  def withVersion(newVersion: String) = copy(version = newVersion)

  import AdapterMappings._

  //NB: Serialize to the old JAXB format, to preseve compatibility with already-deployed files
  def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <AdapterMappings>
      <hostname>{ Unknown }</hostname>
      <timestamp>{ XmlDateHelper.now }</timestamp>
      <version>{ version }</version>
      <mappings>
        {
          for {
            (networkTerm, localTerms) <- mappings
          } yield {
            <entry>
              <key>{ networkTerm }</key>
              <value>
                { localTerms.map(localTerm => <local_key>{ localTerm }</local_key>) }
              </value>
            </entry>
          }
        }
      </mappings>
    </AdapterMappings>
  }

  def toCsv: String = {
    val pairs = for {
      (shrineTerm, locals) <- mappings.toSeq
      localTerm <- locals
    } yield {
      Array(shrineTerm, localTerm)
    }

    val writer = new StringWriter

    try {
      val csvWriter = new CSVWriter(writer)

      pairs.foreach(csvWriter.writeNext)
    } finally { writer.close() }

    writer.toString
  }
}

object AdapterMappings {
  val Unknown = "Unknown"

  val empty = new AdapterMappings

  def fromXml(xml: NodeSeq): Try[AdapterMappings] = {
    val entries = xml \ "mappings" \ "entry"

    val tupleAttempts = entries.map { entryXml =>
      for {
        networkTerm <- Try((entryXml \ "key").text.trim)
        localTerms <- Try((entryXml \ "value" \ "local_key").map(_.text.trim))
      } yield (networkTerm, localTerms.toSet)
    }

    val versionText = (xml \ "version").text.trim

    val version = if (versionText.isEmpty) { Unknown } else versionText

    for {
      tuples <- Tries.sequence(tupleAttempts)
    } yield AdapterMappings(version, tuples.toMap)
  }

  def fromCsv(reader: Reader): Try[AdapterMappings] = Try {
    //XXX: FIXME: Get this from a/the file somehow
    val version = Unknown

    try {
      val lines = Csv.lazySlurp(reader)

      empty.withVersion(version) ++ lines
    } finally {
      reader.close()
    }
  }
}
