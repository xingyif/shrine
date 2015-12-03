package net.shrine.config.mappings

import net.shrine.log.Loggable

import scala.util.Try
import scala.xml.XML
import scala.util.control.NonFatal
import net.shrine.util.XmlDateHelper.time

trait FormatDetectingAdapterMappingsSource extends AdapterMappingsSource with Loggable { self: ReaderAdapterMappingsSource =>
  override def load: Try[AdapterMappings] = {

    info(s"Loading adapter mappings from $mappingFileName")

    import FormatDetectingAdapterMappingsSource.Implicits._

    def tryAsXml: Try[AdapterMappings] = Try(XML.load(reader)).flatMap(AdapterMappings.fromXml).ifSuccessful("Detected XML adapter mappings format")

    def tryAsCsv: Try[AdapterMappings] = AdapterMappings.fromCsv(reader).ifSuccessful("Detected CSV adapter mappings format")

    time("Loading adapter mappings")(debug(_)) {
      tryAsXml.orElse(tryAsCsv).ifFailure(s"Couldn't load adapter mappings from $mappingFileName")
    }
  }
}

object FormatDetectingAdapterMappingsSource extends Loggable {
  object Implicits {
    final implicit class WithLoggingOps[T](val attempt: Try[T]) extends AnyVal {
      def ifSuccessful(message: String): Try[T] = {
        if (attempt.isSuccess) { info(message) }

        attempt
      }

      def ifFailure(message: String): Try[T] = {
        if (attempt.isFailure) { warn(message) }

        attempt
      }
    }
  }
}