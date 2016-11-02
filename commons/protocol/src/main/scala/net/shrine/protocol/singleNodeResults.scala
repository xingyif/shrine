package net.shrine.protocol

import scala.concurrent.duration.Duration
import scala.util.Try
import scala.xml.NodeSeq
import net.shrine.serialization.XmlMarshaller
import net.shrine.serialization.XmlUnmarshaller
import net.shrine.util.{StackTrace, XmlUtil, NodeSeqEnrichments, DurationEnrichments}

import DurationEnrichments._

/**
 * @author clint
 * @since Nov 1, 2013
 */
sealed abstract class SingleNodeResult(val origin: NodeId) extends XmlMarshaller

object SingleNodeResult {
  type Unmarshaller = Set[ResultOutputType] => NodeSeq => Try[SingleNodeResult]
  
  private val unmarshallers: Map[String, Unmarshaller] = Map(
    Timeout.rootTagName -> Timeout.fromXml _,
    FailureResult.rootTagName -> FailureResult.fromXml _,
    Result.rootTagName -> Result.fromXml _)

  //TODO: TEST!
  def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[SingleNodeResult] = {
    for {
      rootTagName <- Try(xml.head.label)
      if unmarshallers.contains(rootTagName)
      unmarshaller = unmarshallers(rootTagName)
      unmarshalled <- unmarshaller(breakdownTypes)(xml)
    } yield {
      unmarshalled
    }
  }
}

sealed abstract class SingleNodeResultCompanion[R](override val rootTagName: String) extends HasRootTagName {
  def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[R]
}

/**
 * @author clint
 * @since Nov 1, 2013
 */
final case class Timeout(override val origin: NodeId) extends SingleNodeResult(origin) {
  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    XmlUtil.renameRootTag(Timeout.rootTagName) {
      <shrineTimeout>
        { origin.toXml }
      </shrineTimeout>
    }
  }
}

object Timeout extends SingleNodeResultCompanion[Timeout]("shrineTimeout") {
  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[Timeout] = {
    import NodeSeqEnrichments.Strictness._

    for {
      origin <- xml.withChild(NodeId.rootTagName).flatMap(NodeId.fromXml)
    } yield Timeout(origin)
  }
}

/**
 * @author clint
 * @since Nov 1, 2013
 */
final case class FailureResult(override val origin: NodeId, cause: Throwable) extends SingleNodeResult(origin) {
  //NB: Sidestep serializing throwables by just serializing the cause's message and stack trace as a big string
  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    XmlUtil.renameRootTag(FailureResult.rootTagName) {
      <shrineFailure>
        { origin.toXml }
        <cause>{ s"$cause : ${StackTrace.stackTraceAsString(cause)}" }</cause>
      </shrineFailure>
    }
  }
}

object FailureResult extends SingleNodeResultCompanion[FailureResult]("shrineFailure") {
  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[FailureResult] = {
    import NodeSeqEnrichments.Strictness._

    for {
      origin <- xml.withChild(NodeId.rootTagName).flatMap(NodeId.fromXml)
    //TODO see SHRINE-1486
      //NB: Sidestep serializing throwables by just serializing the cause's message and stack trace as a big string
      cause <- xml.withChild("cause").map(_.text)
    } yield FailureResult(origin, new Exception(cause) with scala.util.control.NoStackTrace) //todo looks like the stack trace gets dropped here
  }
}

/**
 * @author clint
 * @since Nov 1, 2013
 */
final case class Result(override val origin: NodeId, elapsed: Duration, response: BaseShrineResponse) extends SingleNodeResult(origin) {
  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    XmlUtil.renameRootTag(Result.rootTagName) {
      <shrineResult>
        { origin.toXml }
        { XmlUtil.renameRootTag("elapsed")(elapsed.toXml) }
        <response>
          { response.toXml }
        </response>
      </shrineResult>
    }
  }
}

object Result extends SingleNodeResultCompanion[Result]("shrineResult") {
  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[Result] = {
    import NodeSeqEnrichments.Strictness._

    for {
      origin <- xml.withChild(NodeId.rootTagName).flatMap(NodeId.fromXml)
      elapsed <- xml.withChild("elapsed").flatMap(Duration.fromXml)
      responseXml <- xml.withChild("response")
      response <- BaseShrineResponse.fromXml(breakdownTypes)(responseXml \ "_")
    } yield Result(origin, elapsed, response)
  }
}
