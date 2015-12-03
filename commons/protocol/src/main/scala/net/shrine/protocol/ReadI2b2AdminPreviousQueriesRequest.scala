package net.shrine.protocol

import scala.concurrent.duration.Duration
import scala.util.Try
import scala.xml.NodeSeq
import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.serialization.I2b2Unmarshaller
import net.shrine.util.SEnum
import net.shrine.util.XmlDateHelper
import net.shrine.util.XmlUtil
import net.shrine.util.NodeSeqEnrichments
import scala.xml.Node
import net.shrine.serialization.I2b2UnmarshallingHelpers

/**
 * @author clint
 * @date Mar 29, 2013
 */
final case class ReadI2b2AdminPreviousQueriesRequest(
  override val projectId: String,
  override val waitTime: Duration,
  override val authn: AuthenticationInfo,
  username: ReadI2b2AdminPreviousQueriesRequest.Username,
  searchString: String,
  maxResults: Int,
  startDate: Option[XMLGregorianCalendar],
  sortOrder: ReadI2b2AdminPreviousQueriesRequest.SortOrder = ReadI2b2AdminPreviousQueriesRequest.SortOrder.Descending,
  searchStrategy: ReadI2b2AdminPreviousQueriesRequest.Strategy = ReadI2b2AdminPreviousQueriesRequest.Strategy.Contains,
  categoryToSearchWithin: ReadI2b2AdminPreviousQueriesRequest.Category = ReadI2b2AdminPreviousQueriesRequest.Category.Top) extends ShrineRequest(projectId, waitTime, authn) with CrcRequest with HandleableAdminShrineRequest {

  override val requestType = RequestType.ReadI2b2AdminPreviousQueriesRequest

  override def handleAdmin(handler: I2b2AdminRequestHandler, shouldBroadcast: Boolean): ShrineResponse = handler.readI2b2AdminPreviousQueries(this, shouldBroadcast)

  protected override def i2b2MessageBody = XmlUtil.stripWhitespace {
    <message_body>
      { i2b2PsmHeader }
      <ns4:get_name_info category={ categoryToSearchWithin.toString } max={ maxResults.toString }>
        <match_str strategy={ searchStrategy.toString }>{ searchString }</match_str>
        { startDate.map(d => <create_date>{ d }</create_date>).orNull }
        { username.toI2b2 }
        <ascending>{ sortOrder.isAscending }</ascending>
      </ns4:get_name_info>
    </message_body>
  }

  override def toXml = XmlUtil.stripWhitespace {
    <readAdminPreviousQueries>
      { headerFragment }
      { username.toXml }
      <searchString>{ searchString }</searchString>
      { startDate.map(d => <startDate>{ d }</startDate>).orNull }
      <maxResults>{ maxResults }</maxResults>
      <sortOrder>{ sortOrder }</sortOrder>
      <categoryToSearchWithin>{ categoryToSearchWithin }</categoryToSearchWithin>
      <searchStrategy>{ searchStrategy }</searchStrategy>
    </readAdminPreviousQueries>
  }
}

object ReadI2b2AdminPreviousQueriesRequest extends I2b2XmlUnmarshaller[ReadI2b2AdminPreviousQueriesRequest] with ShrineXmlUnmarshaller[ReadI2b2AdminPreviousQueriesRequest] with ShrineRequestUnmarshaller with I2b2UnmarshallingHelpers {
  val allUsers = "@"

  def enumValue[T](enumCompanion: SEnum[T])(name: String): Try[T] = Try {
    enumCompanion.valueOf(name).get
  }

  private def parseDate(dateXml: NodeSeq): Try[XMLGregorianCalendar] = XmlDateHelper.parseXmlTime(dateXml.text)

  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ReadI2b2AdminPreviousQueriesRequest] = {
    import NodeSeqEnrichments.Strictness._

    def textIn(tagName: String): Try[String] = xml.withChild(tagName).map(_.text.trim)

    for {
      waitTime <- shrineWaitTime(xml)
      authn <- shrineAuthenticationInfo(xml)
      maxResults <- textIn("maxResults").map(_.toInt)
      sortOrder <- textIn("sortOrder").flatMap(enumValue(SortOrder))
      searchStrategy <- textIn("searchStrategy").flatMap(enumValue(Strategy))
      categoryToSearchWithin <- textIn("categoryToSearchWithin").flatMap(enumValue(Category))
      projectId <- shrineProjectId(xml)
      usernameValue <- textIn("username")
      username = Username.fromValue(usernameValue)
      searchString <- textIn("searchString")
      startDate = xml.withChild("startDate").flatMap(parseDate).toOption
    } yield {
      ReadI2b2AdminPreviousQueriesRequest(
        projectId,
        waitTime,
        authn,
        username,
        searchString,
        maxResults,
        startDate,
        sortOrder,
        searchStrategy,
        categoryToSearchWithin)
    }
  }

  override def fromI2b2(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ReadI2b2AdminPreviousQueriesRequest] = {
    def enumValueFrom[T](enumCompanion: SEnum[T])(elem: Try[NodeSeq]): Try[T] = {
      for {
        name <- elem.map(_.text)
        result <- enumValue(enumCompanion)(name)
      } yield result
    }

    import NodeSeqEnrichments.Strictness._

    for {
      projectId <- i2b2ProjectId(xml)
      waitTime <- i2b2WaitTime(xml)
      authn <- i2b2AuthenticationInfo(xml)
      messageBody = xml withChild "message_body"
      getNameInfo = messageBody withChild "get_name_info"
      matchStrElem = getNameInfo withChild "match_str"
      userId <- (getNameInfo withChild "user_id").map(_.text.trim)
      username = Username.fromI2b2Value(userId)
      matchStr <- (getNameInfo withChild "match_str").map(_.text.trim)
      max <- getNameInfo.map(xml => (xml \ "@max").text.toInt)
      createDateOption <- getNameInfo.map(xml => (xml \ "create_date").headOption.flatMap(parseDate(_).toOption))
      sortOrder = if ((getNameInfo withChild "ascending").map(_.text.toBoolean).getOrElse(false)) SortOrder.Ascending else SortOrder.Descending
      strategy <- enumValueFrom(Strategy)(matchStrElem.map(_ \ "@strategy"))
      category <- enumValueFrom(Category)(getNameInfo.map(_ \ "@category"))
    } yield {
      ReadI2b2AdminPreviousQueriesRequest(
        projectId,
        waitTime,
        authn,
        username,
        matchStr,
        max,
        createDateOption,
        sortOrder,
        strategy,
        category)
    }
  }

  sealed trait Username {
    def value: String

    def toI2b2: Node
    
    def toXml: Node
    
    def isExact: Boolean
    
    final def isExcept: Boolean = !isExact
  }

  object Username {
    private val i2b2ExceptPrefix = "@-"
    private val exceptPrefix = "-"

    val All = Exactly("@")
      
    def fromI2b2Value(value: String): Username = from(i2b2ExceptPrefix)(value)
    
    def fromValue(value: String): Username = from(exceptPrefix)(value)
    
    private def from(prefix: String)(value: String): Username = {
      if (value.startsWith(prefix)) {
        Except(value.drop(prefix.size))
      } else {
        Exactly(value)
      }
    }

    final case class Exactly(value: String) extends Username {
      override def toI2b2: Node = <user_id>{ value }</user_id>
      
      override def toXml: Node = <username>{ value }</username>
      
      override def isExact: Boolean = true
    }

    final case class Except(value: String) extends Username {
      override def toI2b2: Node = <user_id>{ s"@-$value" }</user_id>
      
      override def toXml: Node = <username>{ s"-$value" }</username>
      
      override def isExact: Boolean = false
    }
  }

  final case class Category private[Category] (override val name: String, isFlagged: Boolean = false) extends Category.Value

  object Category extends SEnum[Category] {
    val All = Category("@")
    val Top = Category("top")
    val Results = Category("results")
    val Pdo = Category("pdo")
    val Flagged = Category("flagged", true)
  }

  final case class Strategy private[Strategy] (override val name: String, compare: (String, String) => Boolean) extends Strategy.Value {
    def isMatch(field: String, searchTerm: String): Boolean = compare(field, searchTerm)
  }

  object Strategy extends SEnum[Strategy] {
    val Exact = Strategy("exact", _ == _)
    val Left = Strategy("left", _.startsWith(_))
    val Right = Strategy("right", _.endsWith(_))
    val Contains = Strategy("contains", _.contains(_))
  }

  final case class SortOrder private[SortOrder] (override val name: String) extends SortOrder.Value {
    def isAscending: Boolean = this == SortOrder.Ascending
    def isDescending: Boolean = this == SortOrder.Descending
  }

  object SortOrder extends SEnum[SortOrder] {
    val Ascending = SortOrder("ascending")
    val Descending = SortOrder("descending")
  }
}