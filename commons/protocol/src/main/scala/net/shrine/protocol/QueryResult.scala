package net.shrine.protocol

import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.problem.{ProblemNotYetEncoded, Problem, ProblemDigest}
import net.shrine.protocol.QueryResult.StatusType

import scala.xml.NodeSeq
import net.shrine.util.{Tries, XmlUtil, NodeSeqEnrichments, SEnum, XmlDateHelper, OptionEnrichments}
import net.shrine.serialization.{ I2b2Marshaller, XmlMarshaller }
import scala.util.Try

/**
 * @author Bill Simons
 * @since 4/15/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 *
 * NB: this is a case class to get a structural equality contract in hashCode and equals, mostly for testing
 */
final case class QueryResult(
  resultId: Long,
  instanceId: Long,
  resultType: Option[ResultOutputType],
  setSize: Long,
  startDate: Option[XMLGregorianCalendar],
  endDate: Option[XMLGregorianCalendar],
  description: Option[String],
  statusType: StatusType,
  statusMessage: Option[String],
  problemDigest: Option[ProblemDigest] = None,
  breakdowns: Map[ResultOutputType,I2b2ResultEnvelope] = Map.empty
) extends XmlMarshaller with I2b2Marshaller {

  def this(
            resultId: Long,
            instanceId: Long,
            resultType: ResultOutputType,
            setSize: Long,
            startDate: XMLGregorianCalendar,
            endDate: XMLGregorianCalendar,
            statusType: QueryResult.StatusType) = {
    this(
      resultId,
      instanceId,
      Option(resultType),
      setSize,
      Option(startDate),
      Option(endDate),
      None, //description
      statusType,
      None) //statusMessage 
  }

  def this(
            resultId: Long,
            instanceId: Long,
            resultType: ResultOutputType,
            setSize: Long,
            startDate: XMLGregorianCalendar,
            endDate: XMLGregorianCalendar,
            description: String,
            statusType: QueryResult.StatusType) = {
    this(
      resultId,
      instanceId,
      Option(resultType),
      setSize,
      Option(startDate),
      Option(endDate),
      Option(description),
      statusType,
      None) //statusMessage
  }

  def resultTypeIs(testedResultType: ResultOutputType): Boolean = resultType match {
    case Some(rt) => rt == testedResultType
    case _ => false
  }

  import QueryResult._

  //NB: Fragile, non-type-safe ==
  def isError = statusType == StatusType.Error

  def elapsed: Option[Long] = {
    def inMillis(xmlGc: XMLGregorianCalendar) = xmlGc.toGregorianCalendar.getTimeInMillis

    for {
      start <- startDate
      end <- endDate
    } yield inMillis(end) - inMillis(start)
  }

  //Sorting isn't strictly necessary, but makes deterministic unit testing easier.  
  //The number of breakdowns will be at most 4, so performance should not be an issue.
  private def sortedBreakdowns: Seq[I2b2ResultEnvelope] = {
    breakdowns.values.toSeq.sortBy(_.resultType.name)
  }

  override def toI2b2: NodeSeq = {
    import OptionEnrichments._

    XmlUtil.stripWhitespace {
      <query_result_instance>
        <result_instance_id>{ resultId }</result_instance_id>
        <query_instance_id>{ instanceId }</query_instance_id>
        { description.toXml(<description/>) }
        {
          resultType match {
            case Some(rt) if !rt.isError => //noinspection RedundantBlock
            {
              if (rt.isBreakdown) { rt.toI2b2NameOnly() }
              else { rt.toI2b2 }
            }
            case _ => ResultOutputType.ERROR.toI2b2NameOnly("")
          }
        }
        <set_size>{ setSize }</set_size>
        { startDate.toXml(<start_date/>) }
        { endDate.toXml(<end_date/>) }
        <query_status_type>
          <name>{ statusType }</name>
          { statusType.toI2b2(this) }
        </query_status_type>
        {
          //NB: Deliberately use Shrine XML format instead of the i2b2 one.  Adding breakdowns to i2b2-format XML here is deviating from the i2b2 XSD schema in any case,
          //so if we're going to do that, let's produce saner XML.
          sortedBreakdowns.map(_.toXml.head).map(XmlUtil.renameRootTag("breakdown_data"))
        }
      </query_result_instance>
    }
  }

  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    import OptionEnrichments._
    
    <queryResult>
      <resultId>{ resultId }</resultId>
      <instanceId>{ instanceId }</instanceId>
      { resultType.toXml(_.toXml) }
      <setSize>{ setSize }</setSize>
      { startDate.toXml(<startDate/>) }
      { endDate.toXml(<endDate/>) }
      { description.toXml(<description/>) }
      <status>{ statusType }</status>
      { statusMessage.toXml(<statusMessage/>) }
      {
        //Sorting isn't strictly necessary, but makes deterministic unit testing easier.  
        //The number of breakdowns will be at most 4, so performance should not be an issue. 
        sortedBreakdowns.map(_.toXml)
      }
      { problemDigest.map(_.toXml).getOrElse("") }
    </queryResult>
  }

  def withId(id: Long): QueryResult = copy(resultId = id)

  def withInstanceId(id: Long): QueryResult = copy(instanceId = id)

  def modifySetSize(f: Long => Long): QueryResult = withSetSize(f(setSize))
  
  def withSetSize(size: Long): QueryResult = copy(setSize = size)

  def withDescription(desc: String): QueryResult = copy(description = Option(desc))

  def withResultType(resType: ResultOutputType): QueryResult = copy(resultType = Option(resType))

  def withBreakdown(breakdownData: I2b2ResultEnvelope) = copy(breakdowns = breakdowns + (breakdownData.resultType -> breakdownData))

  def withBreakdowns(newBreakdowns: Map[ResultOutputType, I2b2ResultEnvelope]) = copy(breakdowns = newBreakdowns)
}

object QueryResult {

  final case class StatusType(
    name: String,
    isDone: Boolean,
    i2b2Id: Option[Int] = Some(-1),
    private val doToI2b2:(QueryResult => NodeSeq) = StatusType.defaultToI2b2) extends StatusType.Value {

    def isError = this == StatusType.Error

    def toI2b2(queryResult: QueryResult): NodeSeq = doToI2b2(queryResult)
  }

  object StatusType extends SEnum[StatusType] {
    private val defaultToI2b2: QueryResult => NodeSeq = { queryResult =>
      val i2b2Id: Int = queryResult.statusType.i2b2Id.getOrElse{
        throw new IllegalStateException(s"queryResult.statusType ${queryResult.statusType} has no i2b2Id")
      }
      <status_type_id>{ i2b2Id }</status_type_id><description>{ queryResult.statusType.name }</description>
    }

    val noMessage:NodeSeq = null
    val Error = StatusType("ERROR", isDone = true, None, { queryResult =>
      (queryResult.statusMessage, queryResult.problemDigest) match {
        case (Some(msg),Some(pd)) => <description>{ msg }</description> ++ pd.toXml
        case (Some(msg),None) => <description>{ msg }</description>
        case (None,Some(pd)) => pd.toXml
        case (None, None) => noMessage
      }
    })
    /*
      msg =>
        <codec>net.shrine.something.is.Broken</codec>
          <summary>Something is borked</summary>
          <description>{ msg }</description>
          <details>Herein is a stack trace, multiple lines</details>
    ))
    */
    val Finished = StatusType("FINISHED", isDone = true, Some(3))
    //TODO: Can we use the same <status_type_id> for Queued, Processing, and Incomplete?
    val Processing = StatusType("PROCESSING", isDone = false, Some(2))
    val Queued = StatusType("QUEUED", isDone = false, Some(2))
    val Incomplete = StatusType("INCOMPLETE", isDone = false, Some(2))
    //TODO: What <status_type_id>s should these have?  Does anyone care?
    val Held = StatusType("HELD", isDone = false)
    val SmallQueue = StatusType("SMALL_QUEUE", isDone = false)
    val MediumQueue = StatusType("MEDIUM_QUEUE", isDone = false)
    val LargeQueue = StatusType("LARGE_QUEUE", isDone = false)
    val NoMoreQueue = StatusType("NO_MORE_QUEUE", isDone = false)
  }

  def extractLong(nodeSeq: NodeSeq)(elemName: String): Long = (nodeSeq \ elemName).text.toLong

  private def parseDate(lexicalRep: String): Option[XMLGregorianCalendar] = XmlDateHelper.parseXmlTime(lexicalRep).toOption

  def elemAt(path: String*)(xml: NodeSeq): NodeSeq = path.foldLeft(xml)(_ \ _)

  def asText(path: String*)(xml: NodeSeq): String = elemAt(path: _*)(xml).text.trim

  def asResultOutputTypeOption(elemNames: String*)(breakdownTypes: Set[ResultOutputType], xml: NodeSeq): Option[ResultOutputType] = {
    import ResultOutputType.valueOf

    val typeName = asText(elemNames: _*)(xml)

    valueOf(typeName) orElse valueOf(breakdownTypes)(typeName)
  }

  def extractResultOutputType(xml: NodeSeq)(parse: NodeSeq => Try[ResultOutputType]): Option[ResultOutputType] = {
    val attempt = parse(xml)
    
    attempt.toOption
  }

  def extractProblemDigest(xml: NodeSeq):Option[ProblemDigest] = {

    val subXml = xml \ "problem"
    if(subXml.nonEmpty) Some(ProblemDigest.fromXml(xml))
    else None
  }

  def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): QueryResult = {
    def extract(elemName: String): Option[String] = {
      Option((xml \ elemName).text.trim).filter(!_.isEmpty)
    }

    def extractDate(elemName: String): Option[XMLGregorianCalendar] = extract(elemName).flatMap(parseDate)

    val asLong = extractLong(xml) _

    import NodeSeqEnrichments.Strictness._
    import Tries.sequence

    def extractBreakdowns(elemName: String): Map[ResultOutputType, I2b2ResultEnvelope] = {
      //noinspection ScalaUnnecessaryParentheses
      val mapAttempt = for {
        subXml <- xml.withChild(elemName)
        envelopes <- sequence(subXml.map(I2b2ResultEnvelope.fromXml(breakdownTypes)))
        mappings = envelopes.map(envelope => (envelope.resultType -> envelope))
      } yield Map.empty ++ mappings

      mapAttempt.getOrElse(Map.empty)
    }

    QueryResult(
      resultId = asLong("resultId"),
      instanceId = asLong("instanceId"),
      resultType = extractResultOutputType(xml \ "resultType")(ResultOutputType.fromXml),
      setSize = asLong("setSize"),
      startDate = extractDate("startDate"),
      endDate = extractDate("endDate"),
      description = extract("description"),
      statusType = StatusType.valueOf(asText("status")(xml)).get, //TODO: Avoid fragile .get call
      statusMessage = extract("statusMessage"),
      problemDigest = extractProblemDigest(xml),
      breakdowns = extractBreakdowns("resultEnvelope")
    )
  }

  def fromI2b2(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): QueryResult = {
    def asLong = extractLong(xml) _

    def asTextOption(path: String*): Option[String] = elemAt(path: _*)(xml).headOption.map(_.text.trim)

    def asXmlGcOption(path: String): Option[XMLGregorianCalendar] = asTextOption(path).filter(!_.isEmpty).flatMap(parseDate)

    QueryResult(
      resultId = asLong("result_instance_id"),
      instanceId = asLong("query_instance_id"),
      resultType = extractResultOutputType(xml \ "query_result_type")(ResultOutputType.fromI2b2),
      setSize = asLong("set_size"),
      startDate = asXmlGcOption("start_date"),
      endDate = asXmlGcOption("end_date"),
      description = asTextOption("description"),
      statusType = StatusType.valueOf(asText("query_status_type", "name")(xml)).get, //TODO: Avoid fragile .get call
      statusMessage = asTextOption("query_status_type", "description"),
      problemDigest = extractProblemDigest(xml \ "query_status_type"))
  }

  def errorResult(description: Option[String], statusMessage: String,problemDigest:ProblemDigest):QueryResult = {
    QueryResult(
      resultId = 0L,
      instanceId = 0L,
      resultType = None,
      setSize = 0L,
      startDate = None,
      endDate = None,
      description = description,
      statusType = StatusType.Error,
      statusMessage = Option(statusMessage),
      problemDigest = Option(problemDigest))
  }

  def errorResult(description: Option[String], statusMessage: String,problem:Option[Problem] = None):QueryResult = {

    val problemDigest = problem.getOrElse(ProblemNotYetEncoded(statusMessage)).toDigest

    QueryResult(
      resultId = 0L,
      instanceId = 0L,
      resultType = None,
      setSize = 0L,
      startDate = None,
      endDate = None,
      description = description,
      statusType = StatusType.Error,
      statusMessage = Option(statusMessage),
      problemDigest = Option(problemDigest))
  }

  /**
   * For reconstituting errorResults from a database
   */
  //todo remove and replace with real Problems
  def errorResult(description:Option[String], statusMessage:String, codec:String,stampText:String, summary:String, digestDescription:String,detailsXml:NodeSeq): QueryResult = {

    val problemDigest = ProblemDigest(codec,stampText,summary,digestDescription,detailsXml)

    QueryResult(
      resultId = 0L,
      instanceId = 0L,
      resultType = None,
      setSize = 0L,
      startDate = None,
      endDate = None,
      description = description,
      statusType = StatusType.Error,
      statusMessage = Option(statusMessage),
      problemDigest = Option(problemDigest))
  }
}