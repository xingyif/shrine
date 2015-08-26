package net.shrine.aggregation

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer

import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.EventResponse
import net.shrine.protocol.ObservationResponse
import net.shrine.protocol.PatientResponse
import net.shrine.protocol.ReadPdoResponse
import net.shrine.protocol.Result
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.SingleNodeResult

/**
 * @author ???
 * @date ???
 */
class ReadPdoResponseAggregator extends Aggregator {

  override def aggregate(results: Iterable[SingleNodeResult], errors: Iterable[ErrorResponse]): ShrineResponse = {
    val eventBuffer: Buffer[EventResponse] = new ArrayBuffer
    val patientBuffer: Buffer[PatientResponse] = new ArrayBuffer
    val observationBuffer: Buffer[ObservationResponse] = new ArrayBuffer

    //TODO handle errors
    results.collect { case result @ Result(_, _, readPdoResponse: ReadPdoResponse) => (result, readPdoResponse) }.foreach {
      case (result, readPdoResponse) => {

        val response = transform(readPdoResponse, result)

        eventBuffer ++= response.events
        patientBuffer ++= response.patients
        observationBuffer ++= response.observations
      }
    }

    //TODO: What to do with passed-in errors?

    ReadPdoResponse(eventBuffer, patientBuffer, observationBuffer)
  }

  /**
   * This method is a no-op transformation but subclasses can override it
   * and do something more interesting.
   */
  protected def transform(entry: ReadPdoResponse, metadata: Result): ReadPdoResponse = entry
}