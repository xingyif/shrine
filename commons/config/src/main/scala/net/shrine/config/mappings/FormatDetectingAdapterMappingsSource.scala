package net.shrine.config.mappings

import net.shrine.log.Loggable

import scala.util.{Failure, Try}
import scala.xml.XML
import net.shrine.util.XmlDateHelper.time

import scala.util.control.NonFatal

trait FormatDetectingAdapterMappingsSource extends AdapterMappingsSource with Loggable { self: ReaderAdapterMappingsSource =>
  override def load(ignored:String): Try[AdapterMappings] = {

    info(s"Loading adapter mappings from $mappingFileName")

    import FormatDetectingAdapterMappingsSource.Implicits._

    def tryAsXml: Try[AdapterMappings] = Try(XML.load(reader)).flatMap(AdapterMappings.fromXml(mappingFileName,_)).ifSuccessful("Detected XML adapter mappings format")

    def tryAsCsv: Try[AdapterMappings] = AdapterMappings.fromCsv(mappingFileName,reader).ifSuccessful("Detected CSV adapter mappings format")

    time("Loading adapter mappings")(debug(_)) {
      tryAsXml.orElse(tryAsCsv).recoverWith{
          case NonFatal(x) =>
            error(s"Failed to load $mappingFileName as XML and as a CSV due to $x",x)
            Failure(x)
      }
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