package net.shrine.protocol

import scala.xml.NodeSeq
import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import ReadI2b2AdminPreviousQueriesRequest.Category
import ReadI2b2AdminPreviousQueriesRequest.SortOrder
import ReadI2b2AdminPreviousQueriesRequest.Strategy
import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.util.XmlDateHelper
import net.shrine.util.XmlUtil
import scala.util.Try

/**
 * @author clint
 * @date Apr 3, 2013
 */
final class ReadI2b2AdminPreviousQueriesRequestTest extends ShouldMatchersForJUnit {
  val projectId = "some-projectid"

  import scala.concurrent.duration._

  val waitTime = 999999L.milliseconds

  val authn = AuthenticationInfo("d", "u", Credential("p", false))
  val searchString = "aksdhjksadhjksadhksadh"
  val maxResults = 654321
  val user = "a-user-whose-queries-we-want"
  val now = XmlDateHelper.now

  import ReadI2b2AdminPreviousQueriesRequest._

  @Test
  def testToXml = doTestToXml(makeShrineXml, _.toXml)

  @Test
  def testToI2b2 = doTestToXml(makeI2b2Xml, _.toI2b2)

  @Test
  def testFromXml = doTestFromXml(makeShrineXml, ReadI2b2AdminPreviousQueriesRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet))

  @Test
  def testFromI2b2 = doTestFromXml(makeI2b2Xml, ReadI2b2AdminPreviousQueriesRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet))

  @Test
  def testToXmlRoundTrip {
    doTestRoundTrip(_.toXml, ReadI2b2AdminPreviousQueriesRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet))
  }

  @Test
  def testShrineRequestXmlRoundTrip {
    doTestRoundTrip(_.toXml, ShrineRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet))
  }

  @Test
  def testReadI2b2AdminPreviousQueriesRequestToXmlRoundTrip {
    doTestRoundTrip(_.toXml, ReadI2b2AdminPreviousQueriesRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet))
  }

  @Test
  def testDoubleDispatchingShrineRequestToI2b2RoundTrip {
    doTestRoundTrip(_.toI2b2, HandleableAdminShrineRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet))
  }

  @Test
  def testReadI2b2AdminPreviousQueriesRequestToI2b2RoundTrip {
    doTestRoundTrip(_.toI2b2, ReadI2b2AdminPreviousQueriesRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet))
  }

  import ReadI2b2AdminPreviousQueriesRequest.Username

  private def makeUsername(exact: Boolean): Username = if (exact) Username.Exactly(user) else Username.Except(user)

  private def request(tuple: (Option[XMLGregorianCalendar], SortOrder, Category, Strategy, Boolean)) = {
    val (startDate, sortOrder, category, strategy, exactUsername) = tuple

    ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, makeUsername(exactUsername), searchString, maxResults, startDate, sortOrder, strategy, category)
  }

  private def doTestToXml(makeExpectedXml: (Option[XMLGregorianCalendar], SortOrder, Category, Strategy, Boolean) => NodeSeq, serialize: ReadI2b2AdminPreviousQueriesRequest => NodeSeq) {
    for {
      combo <- flagCombinations
    } {
      val req = request(combo)

      val actualXml = serialize(req).toString

      val expectedXml = makeExpectedXml.tupled(combo).toString

      actualXml should equal(expectedXml)
    }
  }

  private def doTestFromXml(makeExpectedXml: (Option[XMLGregorianCalendar], SortOrder, Category, Strategy, Boolean) => NodeSeq, deserialize: NodeSeq => Try[ReadI2b2AdminPreviousQueriesRequest]) {
    for {
      combo <- flagCombinations
    } {
      val expectedReq = request(combo)

      val inputXml = makeExpectedXml.tupled(combo)

      val actualReq = deserialize(inputXml).get

      actualReq.authn should equal(expectedReq.authn)
      actualReq.categoryToSearchWithin should equal(expectedReq.categoryToSearchWithin)
      actualReq.maxResults should equal(expectedReq.maxResults)
      actualReq.projectId should equal(expectedReq.projectId)
      actualReq.requestType should equal(expectedReq.requestType)
      actualReq.searchStrategy should equal(expectedReq.searchStrategy)
      actualReq.searchString should equal(expectedReq.searchString)
      actualReq.sortOrder should equal(expectedReq.sortOrder)
      actualReq.startDate should equal(expectedReq.startDate)
      actualReq.username should equal(expectedReq.username)
      actualReq.waitTime should equal(expectedReq.waitTime)

      actualReq should equal(expectedReq)
    }
  }

  private def doTestRoundTrip[R](serialize: ReadI2b2AdminPreviousQueriesRequest => NodeSeq, deserialize: NodeSeq => Try[R]) {
    for {
      req <- flagCombinationReqs
    } {
      val xml = serialize(req)

      val unmarshalled = deserialize(xml).get

      unmarshalled should equal(req)
    }
  }

  private def flagCombinationReqs = flagCombinations.map(request)

  private def flagCombinations: Iterable[(Option[XMLGregorianCalendar], SortOrder, Category, Strategy, Boolean)] = {
    for {
      startDate <- Seq(Option(now), None)
      sortOrder <- SortOrder.values
      category <- Category.values
      strategy <- Strategy.values
      exactUsername <- Set(true, false)
    } yield (startDate, sortOrder, category, strategy, exactUsername)
  }

  private def makeShrineXml(startDate: Option[XMLGregorianCalendar], sortOrder: SortOrder, category: Category, strategy: Strategy, exactUsername: Boolean): NodeSeq = XmlUtil.stripWhitespace {
    <readAdminPreviousQueries>
      <projectId>{ projectId }</projectId><waitTimeMs>{ waitTime.toMillis }</waitTimeMs>
      { authn.toXml }
      { makeUsername(exactUsername).toXml }
      <searchString>{ searchString }</searchString>
      { startDate.map(d => <startDate>{ d }</startDate>).orNull }
      <maxResults>{ maxResults }</maxResults>
      <sortOrder>{ sortOrder }</sortOrder>
      <categoryToSearchWithin>{ category }</categoryToSearchWithin>
      <searchStrategy>{ strategy }</searchStrategy>
    </readAdminPreviousQueries>
  }

  private def makeI2b2Xml(startDate: Option[XMLGregorianCalendar], sortOrder: SortOrder, category: Category, strategy: Strategy, exactUsername: Boolean): NodeSeq = XmlUtil.stripWhitespace {
    <ns6:request xmlns:ns4="http://www.i2b2.org/xsd/cell/crc/psm/1.1/" xmlns:ns8="http://sheriff.shrine.net/" xmlns:ns7="http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/" xmlns:ns3="http://www.i2b2.org/xsd/cell/crc/pdo/1.1/" xmlns:ns5="http://www.i2b2.org/xsd/hive/plugin/" xmlns:ns2="http://www.i2b2.org/xsd/hive/pdo/1.1/" xmlns:ns6="http://www.i2b2.org/xsd/hive/msg/1.1/" xmlns:ns99="http://www.i2b2.org/xsd/cell/pm/1.1/" xmlns:ns100="http://www.i2b2.org/xsd/cell/ont/1.1/">
      <message_header>
        <proxy>
          <redirect_url>https://localhost/shrine/QueryToolService/request</redirect_url>
        </proxy>
        <sending_application>
          <application_name>i2b2_QueryTool</application_name>
          <application_version>0.2</application_version>
        </sending_application>
        <sending_facility>
          <facility_name>SHRINE</facility_name>
        </sending_facility>
        <receiving_application>
          <application_name>i2b2_DataRepositoryCell</application_name>
          <application_version>0.2</application_version>
        </receiving_application>
        <receiving_facility>
          <facility_name>SHRINE</facility_name>
        </receiving_facility>
        { authn.toI2b2 }
        <message_type>
          <message_code>Q04</message_code>
          <event_type>EQQ</event_type>
        </message_type>
        <message_control_id>
          <message_num>EQ7Szep1Md11K4E7zEc99</message_num>
          <instance_num>0</instance_num>
        </message_control_id>
        <processing_id>
          <processing_id>P</processing_id>
          <processing_mode>I</processing_mode>
        </processing_id>
        <accept_acknowledgement_type>AL</accept_acknowledgement_type>
        <project_id>{ projectId }</project_id>
        <country_code>US</country_code>
      </message_header>
      <request_header>
        <result_waittime_ms>{ waitTime.toMillis }</result_waittime_ms>
      </request_header>
      <message_body>
        <ns4:psmheader>
          <user login={ authn.username }>{ authn.username }</user>
          <patient_set_limit>0</patient_set_limit>
          <estimated_time>0</estimated_time>
        </ns4:psmheader>
        <ns4:get_name_info category={ category.toString } max={ maxResults.toString }>
          <match_str strategy={ strategy.toString }>{ searchString }</match_str>
          { startDate.map(d => <create_date>{ d }</create_date>).orNull }
          { makeUsername(exactUsername).toI2b2 }
          <ascending>{ sortOrder.isAscending }</ascending>
        </ns4:get_name_info>
      </message_body>
    </ns6:request>
  }

  @Test
  def testCategory: Unit = {
    import Category._

    All.isFlagged should be(false)
    All.name should be("@")

    Top.isFlagged should be(false)
    Top.name should be("top")

    Results.isFlagged should be(false)
    Results.name should be("results")

    Pdo.isFlagged should be(false)
    Pdo.name should be("pdo")

    Flagged.isFlagged should be(true)
    Flagged.name should be("flagged")
  }

  @Test
  def testUsername: Unit = {
    import Username._

    val value = "askjdkasjdh"

    val exactly = Exactly(value)

    exactly.toI2b2.toString should equal(s"<user_id>$value</user_id>")
    exactly.toXml.toString should equal(s"<username>$value</username>")
    exactly.isExact should be(true)
    exactly.isExcept should be(false)

    val except = Except(value)

    except.toI2b2.toString should equal(s"<user_id>@-$value</user_id>")
    except.toXml.toString should equal(s"<username>-$value</username>")
    except.isExact should be(false)
    except.isExcept should be(true)
  }

  @Test
  def testUsernameFrom: Unit = {
    import Username._

    val value = "askjdkasjdh"

    fromI2b2Value(value) should equal(Exactly(value))
    fromValue(value) should equal(Exactly(value))

    fromI2b2Value(s"@-$value") should equal(Except(value))
    fromValue(s"-$value") should equal(Except(value))
  }

  @Test
  def testSortOrder: Unit = {
    import SortOrder._
    
    Ascending.name should be("ascending")
    Descending.name should be("descending")
    
    Ascending.isAscending should be(true)
    Ascending.isDescending should be(false)
    
    Descending.isAscending should be(false)
    Descending.isDescending should be(true)
  }
  
  @Test
  def testStrategy: Unit = {
    import Strategy._
    
    Exact.name should be("exact")
    Left.name should be("left")
    Right.name should be("right")
    Contains.name should be("contains")
    
    Exact.isMatch("xyz", "xyz") should be(true)
    Exact.isMatch("xyz", "xyabc") should be(false)

    Left.isMatch("xyz", "xyz") should be(true)
    Left.isMatch("xyzabc", "xyz") should be(true)
    Left.isMatch("xabc", "ayz") should be(false)
    
    Right.isMatch("xyz", "xyz") should be(true)
    Right.isMatch("abcxyz", "xyz") should be(true)
    Right.isMatch("abcz", "xyz") should be(false)
    
    Contains.isMatch("xyz", "xyz") should be(true)
    Contains.isMatch("xyz", "x") should be(true)
    Contains.isMatch("xyz", "yz") should be(true)
    Contains.isMatch("xyz", "y") should be(true)
    Contains.isMatch("xyz", "abc") should be(false)
  }
}