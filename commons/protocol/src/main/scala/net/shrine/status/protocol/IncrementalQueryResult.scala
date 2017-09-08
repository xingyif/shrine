package net.shrine.status.protocol

import net.shrine.audit.NetworkQueryId
import org.json4s.ShortTypeHints
import org.json4s.native.Serialization

import scala.util.Try

/**
  * Carrier for incremental query result progress for SHRINE-1.23. Should not be considered a model for future work.
  *
  * @author david 
  * @since 9/8/17
  */
case class IncrementalQueryResult(
                                   networkQueryId:NetworkQueryId,
                                   adapterNodeName:String,
                                   statusTypeName:String,
                                   statusMessage:String
                                 ) {

  def toJson:String = {
    Serialization.write(this)(IncrementalQueryResult.formats)
  }

}

object IncrementalQueryResult {
  val formats = Serialization.formats(ShortTypeHints(List(classOf[IncrementalQueryResult])))

  val incrementalQueryResultsEnvelopeContentsType = s"Seq of ${classOf[IncrementalQueryResult].getSimpleName}s"

  def seqToJson(incrementalQueryResults: Seq[IncrementalQueryResult]):String = {
    Serialization.write(incrementalQueryResults)(formats)
  }

  def seqFromJson(jsonString:String):Try[Seq[IncrementalQueryResult]] = Try{
    implicit val formats = IncrementalQueryResult.formats
    Serialization.read[Seq[IncrementalQueryResult]](jsonString)
  }


}