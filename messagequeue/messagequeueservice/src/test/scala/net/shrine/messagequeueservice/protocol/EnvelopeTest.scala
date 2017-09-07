package net.shrine.messagequeueservice.protocol

import org.json4s.ShortTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}
import org.junit.Test

import scala.util.Try

//todo decide which testing library to use

final class EnvelopeTest {

  @Test
  def testEnvelopeJsonRoundTrip() {
    val exampleContents = ExampleContents("test contents")

    implicit val formats = Serialization.formats(ShortTypeHints(List(classOf[Envelope])))

    val testContentsJson: String = exampleContents.asJson

    val expectedEnvelope = Envelope(ExampleContents.getClass.getSimpleName,testContentsJson)
    val envelopeJson = write(expectedEnvelope)(Serialization.formats(ShortTypeHints(List(classOf[Envelope]))))

    val envelope = read[Envelope](envelopeJson)

    assert(expectedEnvelope == envelope)

    val contentsTry: Try[ExampleContents] = envelope.decode[ExampleContents](ExampleContents.readExampleContets)
    val contents = contentsTry.get

    assert(exampleContents.string == contents.string)
  }

}

case class ExampleContents(string:String) {
  def asJson = write(this)(ExampleContents.exampleContentsFormats)
}

object ExampleContents {
  val exampleContentsFormats = Serialization.formats(ShortTypeHints(List(classOf[ExampleContents])))

  def readExampleContets(string:String):ExampleContents = {
    implicit val formats = exampleContentsFormats
    read[ExampleContents](string)
  }

}
