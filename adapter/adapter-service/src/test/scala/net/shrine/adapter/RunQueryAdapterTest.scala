package net.shrine.adapter

import scala.concurrent.duration.DurationInt
import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import ObfuscatorTest.within3
import net.shrine.adapter.dao.AdapterDao
import net.shrine.adapter.dao.squeryl.AbstractSquerylAdapterTest
import net.shrine.adapter.translators.ExpressionTranslator
import net.shrine.adapter.translators.QueryDefinitionTranslator
import net.shrine.client.HttpClient
import net.shrine.client.HttpResponse
import net.shrine.client.Poster
import net.shrine.protocol.{AuthenticationInfo, BaseShrineResponse, BroadcastMessage, CrcRequest, Credential, DefaultBreakdownResultOutputTypes, ErrorResponse, HiveCredentials, I2b2ResultEnvelope, QueryResult, RawCrcRunQueryResponse, ReadResultRequest, ReadResultResponse, ResultOutputType, RunQueryRequest, RunQueryResponse}
import net.shrine.protocol.RawCrcRunQueryResponse.toQueryResultMap
import net.shrine.protocol.DefaultBreakdownResultOutputTypes.PATIENT_AGE_COUNT_XML
import net.shrine.protocol.ResultOutputType.PATIENT_COUNT_XML
import net.shrine.protocol.DefaultBreakdownResultOutputTypes.PATIENT_GENDER_COUNT_XML
import net.shrine.protocol.DefaultBreakdownResultOutputTypes.PATIENT_RACE_COUNT_XML
import net.shrine.protocol.query.OccuranceLimited
import net.shrine.protocol.query.Or
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.util.XmlDateHelper
import net.shrine.util.XmlUtil

import scala.util.Success
import net.shrine.dao.squeryl.SquerylEntryPoint

import scala.concurrent.duration.Duration
import net.shrine.adapter.dao.model.ShrineError
import net.shrine.adapter.dao.model.QueryResultRow
import net.shrine.problem.TestProblem

/**
 * @author Bill Simons
 * @author Clint Gilbert
 * @since 4/19/11
 * @see http://cbmi.med.harvard.edu
 */
final class RunQueryAdapterTest extends AbstractSquerylAdapterTest with ShouldMatchersForJUnit {
  private val queryDef = QueryDefinition("foo", Term("foo"))

  private val broadcastMessageId = 1234563789L
  private val queryId = 123L
  private val expectedNetworkQueryId = 999L
  private val expectedLocalMasterId = queryId.toString
  private val masterId = 99L
  private val instanceId = 456L
  private val resultId = 42L
  private val projectId = "projectId"
  private val setSize = 17L
  private val xmlResultId = 98765L
  private val userId = "userId"
  private val groupId = "groupId"
  private val topicId = "some-topic-id-123-foo"
  private val topicName = "Topic Name"

  private val justCounts = Set(PATIENT_COUNT_XML)

  private val now = XmlDateHelper.now

  private val countQueryResult = QueryResult(resultId, instanceId, Some(PATIENT_COUNT_XML), setSize, Some(now), Some(now), None, QueryResult.StatusType.Finished, None)

  private val dummyBreakdownData = Map("x" -> 99L, "y" -> 42L, "z" -> 3000L)

  private val hiveCredentials = HiveCredentials("some-hive-domain", "hive-username", "hive-password", "hive-project")

  private val authn = AuthenticationInfo("some-domain", "username", Credential("jksafhkjaf", false))

  private val adapterLockoutThreshold = 99

  private val altI2b2ErrorXml = XmlUtil.stripWhitespace {
    <ns5:response xmlns:ns2="http://www.i2b2.org/xsd/hive/pdo/1.1/" xmlns:ns4="http://www.i2b2.org/xsd/cell/crc/psm/1.1/" xmlns:ns3="http://www.i2b2.org/xsd/cell/crc/pdo/1.1/" xmlns:tns="http://axis2.crc.i2b2.harvard.edu" xmlns:ns9="http://www.i2b2.org/xsd/cell/ont/1.1/" xmlns:ns5="http://www.i2b2.org/xsd/hive/msg/1.1/" xmlns:ns6="http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/" xmlns:ns7="http://www.i2b2.org/xsd/cell/crc/psm/analysisdefinition/1.1/" xmlns:ns10="http://www.i2b2.org/xsd/hive/msg/result/1.1/" xmlns:ns8="http://www.i2b2.org/xsd/cell/pm/1.1/">
      <message_header>
        <i2b2_version_compatible>1.1</i2b2_version_compatible>
        <hl7_version_compatible>2.4</hl7_version_compatible>
        <sending_application>
          <application_name>edu.harvard.i2b2.crc</application_name>
          <application_version>1.5</application_version>
        </sending_application>
        <sending_facility>
          <facility_name>i2b2 Hive</facility_name>
        </sending_facility>
        <receiving_application>
          <application_name>i2b2_QueryTool</application_name>
          <application_version>0.2</application_version>
        </receiving_application>
        <receiving_facility>
          <facility_name>i2b2 Hive</facility_name>
        </receiving_facility>
        <message_control_id>
          <instance_num>1</instance_num>
        </message_control_id>
        <project_id>i2b2</project_id>
      </message_header>
      <response_header>
        <info>Log information</info>
        <result_status>
          <status type="DONE">DONE</status>
          <polling_url interval_ms="100"/>
        </result_status>
      </response_header>
      <message_body>
        <ns4:response xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns4:crc_xml_result_responseType">
          <status>
            <condition type="ERROR">Query result instance id 3126 not found</condition>
          </status>
        </ns4:response>
      </message_body>
    </ns5:response>
  }.toString

  private val otherNetworkId: Long = 12345L

  @Test
  def testProcessRawCrcRunQueryResponseCountQueryOnly: Unit = afterCreatingTables{
    val outputTypes = Set(PATIENT_COUNT_XML)
    
    val translator = new QueryDefinitionTranslator(new ExpressionTranslator(Map("network" -> Set("local1a", "local1b"))))

    val adapter = new RunQueryAdapter(
      Poster("crc-url", null),
      dao,
      hiveCredentials,
      translator,
      adapterLockoutThreshold,
      doObfuscation = false,
      runQueriesImmediately = true,
      breakdownTypes = DefaultBreakdownResultOutputTypes.toSet,
      collectAdapterAudit = false,
      botCountTimeThresholds = Seq.empty,
      obfuscator = Obfuscator(1,1.3,3)
    )

    val request = RunQueryRequest(projectId, 1.second, authn, expectedNetworkQueryId, Option(topicId), Option(topicName), outputTypes, queryDef)

    val networkAuthn = AuthenticationInfo("some-domain", "username", Credential("sadasdasdasd", false))
    
    val broadcastMessage = BroadcastMessage(queryId, networkAuthn, request)
    
    val rawRunQueryResponse = RawCrcRunQueryResponse(
        queryId = queryId, 
        createDate = XmlDateHelper.now,
        userId = request.authn.username, 
        groupId = request.authn.domain, 
        requestXml = request.queryDefinition, 
        queryInstanceId = otherNetworkId,
        singleNodeResults = toQueryResultMap(Seq(countQueryResult)))
    
    val resp = adapter.processRawCrcRunQueryResponse(networkAuthn, request, rawRunQueryResponse).asInstanceOf[RunQueryResponse]

    resp should not be (null)
    
    //Validate the response
    
    resp.createDate should not be(null)
    resp.groupId should be(request.authn.domain)
    resp.userId should be(request.authn.username)
    resp.queryId should be(queryId)
    resp.queryInstanceId should be(otherNetworkId)
    resp.requestXml should equal(request.queryDefinition)
    
    (countQueryResult eq resp.singleNodeResult) should be(false)
    within3(resp.singleNodeResult.setSize, countQueryResult.setSize) should be(true)
    
    resp.singleNodeResult.resultType.get should equal(PATIENT_COUNT_XML)
    
    resp.singleNodeResult.breakdowns should equal(Map.empty)
    
    //validate the DB
    
    val expectedNetworkTerm = queryDef.expr.get.asInstanceOf[Term]

    //We should have one row in the shrine_query table, for the query just performed
    val Seq(queryRow) = list(queryRows)

    {
      queryRow.dateCreated should not be (null)
      queryRow.domain should equal(request.authn.domain)
      queryRow.name should equal(queryDef.name)
      queryRow.localId should equal(expectedLocalMasterId)
      queryRow.networkId should equal(expectedNetworkQueryId)
      queryRow.username should equal(authn.username)
      queryRow.queryDefinition.expr.get should equal(expectedNetworkTerm)
    }

    //We should have one row in the count_result table, with the right obfuscated value, which is within the expected amount from the original count
    val Seq(countRow) = list(countResultRows)

    {
      countRow.creationDate should not be (null)
      countRow.originalValue should equal(countQueryResult.setSize)
      within3(countRow.obfuscatedValue, countRow.originalValue) should be(true)
    }
  }
  
  @Test
  def testProcessRawCrcRunQueryResponseCountAndBreakdownQuery: Unit = afterCreatingTables {
    val allBreakdownTypes = DefaultBreakdownResultOutputTypes.toSet
    
    val breakdownTypes = Seq(PATIENT_GENDER_COUNT_XML)
    
    val outputTypes = Set(PATIENT_COUNT_XML) ++ breakdownTypes
    
    val translator = new QueryDefinitionTranslator(new ExpressionTranslator(Map("network" -> Set("local1a", "local1b"))))

    val request = RunQueryRequest(projectId, 1.second, authn, expectedNetworkQueryId, Option(topicId), Option(topicName), outputTypes, queryDef)

    val networkAuthn = AuthenticationInfo("some-domain", "username", Credential("sadasdasdasd", false))
    
    val broadcastMessage = BroadcastMessage(queryId, networkAuthn, request)
    
    val breakdownQueryResults = breakdownTypes.zipWithIndex.map {
      case (rt, i) =>
        countQueryResult.withId(resultId + i + 1).withResultType(rt)
    }
    
    val singleNodeResults = toQueryResultMap(countQueryResult +: breakdownQueryResults)
    
    val rawRunQueryResponse = RawCrcRunQueryResponse(
        queryId = queryId, 
        createDate = XmlDateHelper.now,
        userId = request.authn.username, 
        groupId = request.authn.domain, 
        requestXml = request.queryDefinition, 
        queryInstanceId = otherNetworkId,
        singleNodeResults = singleNodeResults)

    //Set up our mock CRC
    val poster = Poster("crc-url", new HttpClient {
      def post(input: String, url: String): HttpResponse = HttpResponse.ok {
        (RunQueryRequest.fromI2b2String(allBreakdownTypes)(input) orElse ReadResultRequest.fromI2b2String(allBreakdownTypes)(input)).get match {
          case runQueryReq: RunQueryRequest => rawRunQueryResponse.toI2b2String
          case readResultReq: ReadResultRequest => ReadResultResponse(xmlResultId = 42L, metadata = breakdownQueryResults.head, data = I2b2ResultEnvelope(PATIENT_GENDER_COUNT_XML, dummyBreakdownData)).toI2b2String
          case _ => sys.error(s"Unknown request: '$input'") //Fail loudly
        }
      }
    })
    
    val adapter = RunQueryAdapter(
      poster = poster,
      dao = dao,
      hiveCredentials = hiveCredentials,
      conceptTranslator = translator,
      adapterLockoutAttemptsThreshold = adapterLockoutThreshold,
      doObfuscation = false,
      runQueriesImmediately = true,
      breakdownTypes = DefaultBreakdownResultOutputTypes.toSet,
      collectAdapterAudit = false,
      botCountTimeThresholds = Seq.empty,
      obfuscator = Obfuscator(1,1.3,3)
    )
        
    val resp = adapter.processRawCrcRunQueryResponse(networkAuthn, request, rawRunQueryResponse).asInstanceOf[RunQueryResponse]

    resp should not be (null)
    
    //Validate the response
    
    resp.createDate should not be(null)
    resp.groupId should be(request.authn.domain)
    resp.userId should be(request.authn.username)
    resp.queryId should be(queryId)
    resp.queryInstanceId should be(otherNetworkId)
    resp.requestXml should equal(request.queryDefinition)
    
    (countQueryResult eq resp.singleNodeResult) should be(false)
    within3(resp.singleNodeResult.setSize, countQueryResult.setSize) should be(true)
    
    resp.singleNodeResult.resultType.get should equal(PATIENT_COUNT_XML)
    resp.singleNodeResult.breakdowns.keySet should equal(Set(PATIENT_GENDER_COUNT_XML))
    
    val breakdownEnvelope = resp.singleNodeResult.breakdowns.values.head
    
    breakdownEnvelope.resultType should equal(PATIENT_GENDER_COUNT_XML)
    breakdownEnvelope.data.keySet should equal(dummyBreakdownData.keySet)
    
    //All breakdowns are obfuscated
    for {
      (key, value) <- breakdownEnvelope.data
    } {
      within3(value, dummyBreakdownData(key)) should be(true)
    }
    
    //validate the DB
    
    val expectedNetworkTerm = queryDef.expr.get.asInstanceOf[Term]

    //We should have one row in the shrine_query table, for the query just performed
    val Seq(queryRow) = list(queryRows)

    {
      queryRow.dateCreated should not be (null)
      queryRow.domain should equal(request.authn.domain)
      queryRow.name should equal(queryDef.name)
      queryRow.localId should equal(expectedLocalMasterId)
      queryRow.networkId should equal(expectedNetworkQueryId)
      queryRow.username should equal(authn.username)
      queryRow.queryDefinition.expr.get should equal(expectedNetworkTerm)
    }

    //We should have one row in the count_result table, with the right obfuscated value, which is within the expected amount from the original count
    val Seq(countRow) = list(countResultRows)

    {
      countRow.creationDate should not be (null)
      countRow.originalValue should equal(countQueryResult.setSize)
      within3(countRow.obfuscatedValue, countRow.originalValue) should be(true)
    }

    val breakdownRows @ Seq(xRow, yRow, zRow) = list(breakdownResultRows)

    breakdownRows.map(_.dataKey).toSet should equal(dummyBreakdownData.keySet)
    
    within3(xRow.obfuscatedValue, xRow.originalValue) should be(true)
    xRow.originalValue should be(dummyBreakdownData(xRow.dataKey))
    
    within3(yRow.obfuscatedValue, yRow.originalValue) should be(true)
    yRow.originalValue should be(dummyBreakdownData(yRow.dataKey))
    
    within3(zRow.obfuscatedValue, zRow.originalValue) should be(true)
    zRow.originalValue should be(dummyBreakdownData(zRow.dataKey))
  }

  //NB: See https://open.med.harvard.edu/jira/browse/SHRINE-745
  @Test
  def testParseAltErrorXml {
    val adapter = RunQueryAdapter(
      poster = Poster("crc-url", null),
      dao = null,
      hiveCredentials = hiveCredentials,
      conceptTranslator = null,
      adapterLockoutAttemptsThreshold = adapterLockoutThreshold,
      doObfuscation = false,
      runQueriesImmediately = false,
      breakdownTypes = DefaultBreakdownResultOutputTypes.toSet,
      collectAdapterAudit = false,
      botCountTimeThresholds = Seq.empty,
      obfuscator = Obfuscator(5,6.5,10)
    )

    val resp: ErrorResponse = adapter.parseShrineErrorResponseWithFallback(altI2b2ErrorXml).asInstanceOf[ErrorResponse]

    resp should not be (null)

    resp.errorMessage should be("Query result instance id 3126 not found")
  }

  @Test
  def testParseErrorXml {
    val xml = {
      <ns5:response xmlns:ns2="http://www.i2b2.org/xsd/hive/pdo/1.1/" xmlns:ns4="http://www.i2b2.org/xsd/cell/crc/psm/1.1/" xmlns:ns3="http://www.i2b2.org/xsd/cell/crc/pdo/1.1/" xmlns:tns="http://axis2.crc.i2b2.harvard.edu" xmlns:ns9="http://www.i2b2.org/xsd/cell/ont/1.1/" xmlns:ns5="http://www.i2b2.org/xsd/hive/msg/1.1/" xmlns:ns6="http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/" xmlns:ns7="http://www.i2b2.org/xsd/cell/crc/psm/analysisdefinition/1.1/" xmlns:ns10="http://www.i2b2.org/xsd/hive/msg/result/1.1/" xmlns:ns8="http://www.i2b2.org/xsd/cell/pm/1.1/">
        <message_header>
          <i2b2_version_compatible>1.1</i2b2_version_compatible>
          <hl7_version_compatible>2.4</hl7_version_compatible>
          <sending_application>
            <application_name>edu.harvard.i2b2.crc</application_name>
            <application_version>1.4</application_version>
          </sending_application>
          <sending_facility>
            <facility_name>i2b2 Hive</facility_name>
          </sending_facility>
          <receiving_application>
            <application_name>i2b2web</application_name>
            <application_version>1.4</application_version>
          </receiving_application>
          <receiving_facility>
            <facility_name>i2b2 Hive</facility_name>
          </receiving_facility>
          <message_control_id>
            <instance_num>1</instance_num>
          </message_control_id>
          <project_id>Demo</project_id>
        </message_header>
        <response_header>
          <info>Log information</info>
          <result_status>
            <status type="ERROR">Message error connecting Project Management cell</status>
            <polling_url interval_ms="100"/>
          </result_status>
        </response_header>
        <message_body>
          <ns4:psmheader>
            <user group="i2b2demo" login="admin">admin</user>
            <patient_set_limit>0</patient_set_limit>
            <estimated_time>0</estimated_time>
            <request_type>CRC_QRY_runQueryInstance_fromQueryDefinition</request_type>
          </ns4:psmheader>
          <ns4:request xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns4:query_definition_requestType">
            <query_definition>
              <query_name>Age</query_name>
              <specificity_scale>0</specificity_scale>
              <panel name="Panel_31">
                <panel_number>1</panel_number>
                <panel_accuracy_scale>0</panel_accuracy_scale>
                <invert>0</invert>
                <total_item_occurrences>1</total_item_occurrences>
                <item>
                  <hlevel>2</hlevel>
                  <item_name>Age</item_name>
                  <item_key>\\i2b2\i2b2\Demographics\Age\</item_key>
                  <dim_tablename>concept_dimension</dim_tablename>
                  <dim_columnname>concept_path</dim_columnname>
                  <dim_dimcode>\i2b2\Demographics\Age\</dim_dimcode>
                  <dim_columndatatype>T</dim_columndatatype>
                  <facttablecolumn>concept_cd</facttablecolumn>
                  <item_is_synonym>false</item_is_synonym>
                </item>
              </panel>
            </query_definition>
            <result_output_list>
              <result_output priority_index="1" name="PATIENTSET"/>
            </result_output_list>
          </ns4:request>
        </message_body>
      </ns5:response>
    }.toString

    val adapter = RunQueryAdapter(
      poster = Poster("crc-url", null),
      dao = null,
      hiveCredentials = hiveCredentials,
      conceptTranslator = null,
      adapterLockoutAttemptsThreshold = adapterLockoutThreshold,
      doObfuscation = false,
      runQueriesImmediately = true,
      breakdownTypes = DefaultBreakdownResultOutputTypes.toSet,
      collectAdapterAudit = false,
      botCountTimeThresholds = Seq.empty,
      obfuscator = Obfuscator(5,6.5,10)
    )

    val resp = adapter.parseShrineErrorResponseWithFallback(xml).asInstanceOf[ErrorResponse]

    resp should not be (null)

    resp.errorMessage should not be ("")
  }

  @Test
  def testTranslateNetworkToLocalDoesntLeakCredentialsViaException: Unit = {
    val mappings = Map.empty[String, Set[String]]

    val translator = new QueryDefinitionTranslator(new ExpressionTranslator(mappings))

    val adapter = RunQueryAdapter(
      poster = Poster("crc-url", MockHttpClient),
      dao = null,
      hiveCredentials = null,
      conceptTranslator = translator,
      adapterLockoutAttemptsThreshold = adapterLockoutThreshold,
      doObfuscation = false,
      runQueriesImmediately = true,
      breakdownTypes = DefaultBreakdownResultOutputTypes.toSet,
      collectAdapterAudit = false,
      botCountTimeThresholds = Seq.empty,
      obfuscator = Obfuscator(5,6.5,10)
    )

    val queryDefinition = QueryDefinition("foo", Term("blah"))

    val authn = AuthenticationInfo("d", "u", Credential("p", false))

    val req = RunQueryRequest("projectId", Duration.Inf, authn, otherNetworkId, None, None, Set.empty, queryDef)

    try {
      adapter.translateNetworkToLocal(req)

      fail("Expected an AdapterMappingException")
    } catch {
      case e: AdapterMappingException => {
        e.getMessage.contains(authn.rawToString) should be(false)
        e.getMessage.contains(AuthenticationInfo.elided.toString) should be(true)
      }
    }
  }

  @Test
  def testTranslateQueryDefinitionXml {
    val localTerms = Set("local1a", "local1b")

    val mappings = Map("network" -> localTerms)

    val translator = new QueryDefinitionTranslator(new ExpressionTranslator(mappings))

    val adapter = RunQueryAdapter(
      poster = Poster("crc-url", MockHttpClient),
      dao = null,
      hiveCredentials = null,
      conceptTranslator = translator,
      adapterLockoutAttemptsThreshold = adapterLockoutThreshold,
      doObfuscation = false,
      runQueriesImmediately = true,
      breakdownTypes = DefaultBreakdownResultOutputTypes.toSet,
      collectAdapterAudit = false,
      botCountTimeThresholds = Seq.empty,
      obfuscator = Obfuscator(5,6.5,10)
    )

    val queryDefinition = QueryDefinition("10-17 years old@14:39:20", OccuranceLimited(1, Term("network")))

    val newDef = adapter.conceptTranslator.translate(queryDefinition)

    val expected = QueryDefinition("10-17 years old@14:39:20", Or(Term("local1a"), Term("local1b")))

    newDef should equal(expected)
  }

  @Test
  def testQueuedRegularCountQuery: Unit = afterCreatingTables {
    val adapter = RunQueryAdapter(
      poster = Poster("crc-url", MockHttpClient),
      dao = dao,
      hiveCredentials = null,
      conceptTranslator = null,
      adapterLockoutAttemptsThreshold = adapterLockoutThreshold,
      doObfuscation = false,
      runQueriesImmediately = false,
      breakdownTypes = DefaultBreakdownResultOutputTypes.toSet,
      collectAdapterAudit = false,
      botCountTimeThresholds = Seq.empty,
      obfuscator = Obfuscator(5,6.5,10)
    )

    val networkAuthn = AuthenticationInfo("nd", "nu", Credential("np", false))

    import scala.concurrent.duration._

    val req = RunQueryRequest(projectId, 1.second, authn, expectedNetworkQueryId, Option(topicId), Option(topicName), Set(PATIENT_COUNT_XML), queryDef)

    val broadcastMessage = BroadcastMessage(queryId, networkAuthn, req)

    val resp = adapter.processRequest(broadcastMessage).asInstanceOf[RunQueryResponse]

    resp.groupId should equal(networkAuthn.domain)
    resp.createDate should not be (null) // :\
    resp.queryId should equal(-1L)
    resp.queryInstanceId should equal(-1L)
    resp.requestXml should equal(queryDef)
    resp.userId should equal(networkAuthn.username)

    resp.singleNodeResult.breakdowns should equal(Map.empty)
    resp.singleNodeResult.description.isDefined should be(true)
    resp.singleNodeResult.elapsed should equal(Some(0L))
    resp.singleNodeResult.endDate.isDefined should be(true)
    resp.singleNodeResult.startDate.isDefined should be(true)
    resp.singleNodeResult.instanceId should equal(-1L)
    resp.singleNodeResult.isError should be(false)
    resp.singleNodeResult.resultId should equal(-1L)
    resp.singleNodeResult.resultType should be(Some(PATIENT_COUNT_XML))
    resp.singleNodeResult.setSize should equal(-1L)
    resp.singleNodeResult.statusMessage.isDefined should be(true)
    resp.singleNodeResult.statusType should be(QueryResult.StatusType.Held)
    resp.singleNodeResult.endDate.isDefined should be(true)

    val Some(storedQuery) = dao.findQueryByNetworkId(expectedNetworkQueryId)

    storedQuery.dateCreated should not be (null) // :\
    storedQuery.domain should equal(networkAuthn.domain)
    storedQuery.isFlagged should equal(false)
    storedQuery.localId should equal(-1L.toString)
    storedQuery.name should equal(queryDef.name)
    storedQuery.networkId should equal(expectedNetworkQueryId)
    storedQuery.queryDefinition should equal(queryDef)
    storedQuery.username should equal(networkAuthn.username)
  }

  private def doTestRegularCountQuery(status: QueryResult.StatusType, count: Long) = afterCreatingTables {

    require(!status.isError)

    val countQueryResultToUse = countQueryResult.copy(statusType = status, setSize = count)

    val outputTypes = justCounts

    val resp = doQuery(outputTypes) {
      import RawCrcRunQueryResponse.toQueryResultMap

      RawCrcRunQueryResponse(queryId, now, userId, groupId, queryDef, instanceId, toQueryResultMap(Seq(countQueryResultToUse))).toI2b2String
    }.asInstanceOf[RunQueryResponse]

    doBasicRunQueryResponseTest(resp)

    val firstResult = resp.results.head

    resp.results should equal(Seq(firstResult))

    val Some(savedQuery) = dao.findResultsFor(expectedNetworkQueryId)

    savedQuery.wasRun should equal(true)
    savedQuery.isFlagged should equal(false)

    savedQuery.networkQueryId should equal(expectedNetworkQueryId)
    savedQuery.breakdowns should equal(Nil)
    savedQuery.count.creationDate should not be (null)
    savedQuery.count.localId should equal(countQueryResultToUse.resultId)
    //savedQuery.count.resultId should equal(resultId) TODO: REVISIT
    savedQuery.count.statusType should equal(status)

    if (status.isDone && !status.isError) {
      savedQuery.count.data.get.startDate should not be (null)
      savedQuery.count.data.get.endDate should not be (null)

      savedQuery.count.data.get.originalValue should be(count)
      ObfuscatorTest.within3(savedQuery.count.data.get.obfuscatedValue, count) should be(true)
    } else {
      savedQuery.count.data should be(None)
    }
  }

  @Test
  def testRegularCountQuery = doTestRegularCountQuery(QueryResult.StatusType.Finished, countQueryResult.setSize)

  @Test
  def testRegularCountQueryComesBackProcessing = doTestRegularCountQuery(QueryResult.StatusType.Processing, -1L)

  @Test
  def testRegularCountQueryComesBackQueued = doTestRegularCountQuery(QueryResult.StatusType.Queued, -1L)

  @Test
  def testRegularCountQueryComesBackError = afterCreatingTables {
    val errorQueryResult = QueryResult.errorResult(Some("some-description"), "some-status-message",TestProblem())

    val outputTypes = justCounts

    val resp = doQuery(outputTypes) {
      import RawCrcRunQueryResponse.toQueryResultMap

      RawCrcRunQueryResponse(queryId, now, userId, groupId, queryDef, instanceId, toQueryResultMap(Seq(errorQueryResult))).toI2b2String
    }

    doBasicRunQueryResponseTest(resp)

    //TODO: Why are status and description messages from CRC dropped when unmarshalling QueryResults?
    //resp.results should equal(Seq(errorQueryResult))
    resp.asInstanceOf[RunQueryResponse].results.head.statusType should be(QueryResult.StatusType.Error)

    dao.findResultsFor(expectedNetworkQueryId) should be(None)

    val Some(savedQueryRow) = dao.findQueryByNetworkId(expectedNetworkQueryId)

    val Seq(queryResultRow: QueryResultRow) = {
      import SquerylEntryPoint._

      implicit val breakdownTypes = DefaultBreakdownResultOutputTypes.toSet

      inTransaction {
        from(tables.queryResults) { row =>
          where(row.queryId === savedQueryRow.id).
            select(row.toQueryResultRow)
        }.toSeq
      }
    }

    val Seq(errorRow: ShrineError) = {
      import SquerylEntryPoint._

      inTransaction {
        from(tables.errorResults) { row =>
          where(row.resultId === queryResultRow.id).
            select(row.toShrineError)
        }.toSeq
      }
    }

    errorRow should not be (null)
    //TODO: ErrorMessage
    //errorRow.message should equal(errorQueryResult.statusMessage)
  }

  private def doTestBreakdownsAreObfuscated(result: QueryResult): Unit = {
    result.breakdowns.values.map(_.data).foreach { actualBreakdowns =>
      actualBreakdowns.keySet should equal(dummyBreakdownData.keySet)

      for {
        breakdownName <- actualBreakdowns.keySet
      } {
        within3(actualBreakdowns(breakdownName), dummyBreakdownData(breakdownName)) should be(true)
      }
    }
  }

  @Test
  def testGetBreakdownsWithRegularCountQuery {
    val breakdowns = DefaultBreakdownResultOutputTypes.values.map(breakdownFor)

    val resp = doTestGetBreakdowns(breakdowns)

    val firstResult = resp.results.head

    firstResult.resultType should equal(Some(PATIENT_COUNT_XML))
    firstResult.setSize should equal(setSize)
    firstResult.description should equal(None)
    firstResult.breakdowns.keySet should equal(DefaultBreakdownResultOutputTypes.toSet)
    //NB: Verify that breakdowns are obfuscated
    doTestBreakdownsAreObfuscated(firstResult)

    resp.results.size should equal(1)
  }

  @Test
  def testGetBreakdownsSomeFailures {
    val resultTypesExpectedToSucceed = Seq(PATIENT_AGE_COUNT_XML, PATIENT_GENDER_COUNT_XML)

    val breakdowns = resultTypesExpectedToSucceed.map(breakdownFor)

    val resp = doTestGetBreakdowns(breakdowns)

    val firstResult = resp.results.head

    firstResult.resultType should equal(Some(PATIENT_COUNT_XML))
    firstResult.setSize should equal(setSize)
    firstResult.description should equal(None)
    firstResult.breakdowns.keySet should equal(resultTypesExpectedToSucceed.toSet)
    //NB: Verify that breakdowns are obfuscated
    doTestBreakdownsAreObfuscated(firstResult)

    resp.results.size should equal(1)
  }

  @Test
  def testErrorResponsesArePassedThrough: Unit = {
    val errorResponse = ErrorResponse(TestProblem(summary = "blarg!"))

    val resp = doQuery(Set(PATIENT_COUNT_XML)) {
      errorResponse.toI2b2String
    }

    resp should equal(errorResponse)
  }

  private def breakdownFor(resultType: ResultOutputType) = I2b2ResultEnvelope(resultType, dummyBreakdownData)

  private def doTestGetBreakdowns(successfulBreakdowns: Seq[I2b2ResultEnvelope]): RunQueryResponse = {
    val outputTypes = justCounts ++ DefaultBreakdownResultOutputTypes.toSet

    val resp = doQueryThatReturnsSpecifiedBreakdowns(outputTypes, successfulBreakdowns)

    doBasicRunQueryResponseTest(resp)

    resp
  }

  private def doBasicRunQueryResponseTest(r: BaseShrineResponse) {
    val resp = r.asInstanceOf[RunQueryResponse]

    resp.createDate should equal(now)
    resp.groupId should equal(groupId)
    resp.queryId should equal(queryId)
    resp.queryInstanceId should equal(instanceId)
    resp.queryName should equal(queryDef.name)
    resp.requestXml should equal(queryDef)
  }

  private def doQueryThatReturnsSpecifiedBreakdowns(outputTypes: Set[ResultOutputType], successfulBreakdowns: Seq[I2b2ResultEnvelope]): RunQueryResponse = afterCreatingTablesReturn {
    val breakdownQueryResults = DefaultBreakdownResultOutputTypes.values.zipWithIndex.map {
      case (rt, i) =>
        countQueryResult.withId(resultId + i + 1).withResultType(rt)
    }

    //Need this rigamarole to ensure that resultIds line up such that the type of breakdown the adapter asks for
    //(PATIENT_AGE_COUNT_XML, etc) is what the mock HttpClient actually returns.  Here, we build up maps of QueryResults
    //and I2b2ResultEnvelopes, keyed on resultIds generated in the previous expression, to use to look up values to use
    //to build ReadResultResponses
    val successfulBreakdownsByType = successfulBreakdowns.map(e => e.resultType -> e).toMap

    val successfulBreakdownTypes = successfulBreakdownsByType.keySet

    val breakdownQueryResultsByResultId = breakdownQueryResults.collect { case qr if successfulBreakdownTypes(qr.resultType.get) => qr.resultId -> qr }.toMap

    val breakdownsToBeReturnedByResultId = breakdownQueryResultsByResultId.map {
      case (resultId, queryResult) => (resultId, successfulBreakdownsByType(queryResult.resultType.get))
    }

    val expectedLocalTerm = Term("bar")

    val httpClient = new HttpClient {
      override def post(input: String, url: String): HttpResponse = {
        val resp = CrcRequest.fromI2b2String(DefaultBreakdownResultOutputTypes.toSet)(input) match {
          case Success(req: RunQueryRequest) => {
            //NB: Terms should be translated
            req.queryDefinition.expr.get should equal(expectedLocalTerm)

            //Credentials should be "translated"
            req.authn.username should equal(hiveCredentials.username)
            req.authn.domain should equal(hiveCredentials.domain)

            //I2b2 Project ID should be translated 
            req.projectId should equal(hiveCredentials.projectId)

            val queryResultMap = RawCrcRunQueryResponse.toQueryResultMap(countQueryResult +: breakdownQueryResults)

            RawCrcRunQueryResponse(queryId, now, "userId", "groupId", queryDef, instanceId, queryResultMap)
          }
          //NB: return a ReadResultResponse with new breakdown data each time, but will throw if the asked-for breakdown 
          //is not one of the ones passed to the enclosing method, simulating an error calling the CRC 
          case Success(req: ReadResultRequest) => {
            val resultId = req.localResultId.toLong

            ReadResultResponse(xmlResultId, breakdownQueryResultsByResultId(resultId), breakdownsToBeReturnedByResultId(resultId))
          }
          case _ => ??? //fail loudly
        }

        HttpResponse.ok(resp.toI2b2String)
      }
    }

    val result = doQuery(outputTypes, dao, httpClient)

    validateDb(successfulBreakdowns, breakdownQueryResultsByResultId)

    result.asInstanceOf[RunQueryResponse]
  }

  private def validateDb(breakdownsReturned: Seq[I2b2ResultEnvelope], breakdownQueryResultsByResultId: Map[Long, QueryResult]) {
    val expectedNetworkTerm = Term("foo")

    //We should have one row in the shrine_query table, for the query just performed
    val queryRow = first(queryRows)

    {
      queryRow.dateCreated should not be (null)
      queryRow.domain should equal(authn.domain)
      queryRow.name should equal(queryDef.name)
      queryRow.localId should equal(expectedLocalMasterId)
      queryRow.networkId should equal(expectedNetworkQueryId)
      queryRow.username should equal(authn.username)
      queryRow.queryDefinition.expr.get should equal(expectedNetworkTerm)
    }

    list(queryRows).size should equal(1)

    //We should have one row in the count_result table, with the right obfuscated value, which is within the expected amount from the original count
    val countRow = first(countResultRows)

    {
      countRow.creationDate should not be (null)
      countRow.originalValue should equal(countQueryResult.setSize)
      within3(countRow.obfuscatedValue, countQueryResult.setSize) should be(true)
      within3(countRow.obfuscatedValue, countRow.originalValue) should be(true)
    }

    list(countResultRows).size should equal(1)

    //We should have 5 rows in the query_result table, one for the count result and one for each of the 4 requested breakdown types

    val queryResults = list(queryResultRows)

    {
      val countQueryResultRow = queryResults.find(_.resultType == PATIENT_COUNT_XML).get

      countQueryResultRow.localId should equal(countQueryResult.resultId)
      countQueryResultRow.queryId should equal(queryRow.id)

      val resultIdsByResultType = breakdownQueryResultsByResultId.map { case (resultId, queryResult) => queryResult.resultType.get -> resultId }.toMap

      for (breakdownType <- DefaultBreakdownResultOutputTypes.values) {
        val breakdownQueryResultRow = queryResults.find(_.resultType == breakdownType).get

        breakdownQueryResultRow.queryId should equal(queryRow.id)

        //We'll have a result id if this breakdown type didn't fail
        if (resultIdsByResultType.contains(breakdownQueryResultRow.resultType)) {
          breakdownQueryResultRow.localId should equal(resultIdsByResultType(breakdownQueryResultRow.resultType))
        }
      }
    }

    queryResults.size should equal(5)

    val returnedBreakdownTypes = breakdownsReturned.map(_.resultType).toSet

    val notReturnedBreakdownTypes = DefaultBreakdownResultOutputTypes.toSet -- returnedBreakdownTypes

    val errorResults = list(errorResultRows)

    //We should have a row in the error_result table for each breakdown that COULD NOT be retrieved

    {
      for {
        queryResult <- queryResults
        if notReturnedBreakdownTypes.contains(queryResult.resultType)
        resultType = queryResult.resultType
        resultId = queryResult.id
      } {
        errorResults.find(_.resultId == resultId).isDefined should be(true)
      }
    }

    errorResults.size should equal(notReturnedBreakdownTypes.size)

    //We should have properly-obfuscated rows in the breakdown_result table for each of the breakdown types that COULD be retrieved  
    val breakdownResults = list(breakdownResultRows)

    val bdrs = breakdownResults.toIndexedSeq

    {
      for {
        queryResult <- queryResults
        if returnedBreakdownTypes.contains(queryResult.resultType)
        resultType = queryResult.resultType
        resultId = queryResult.id
      } {
        //Find all the rows for a particular breakdown type
        val rowsWithType = breakdownResults.filter(_.resultId == resultId)

        //Combining the rows should give the expected dummy data
        rowsWithType.map(row => row.dataKey -> row.originalValue).toMap should equal(dummyBreakdownData)

        for (breakdownRow <- rowsWithType) {
          within3(breakdownRow.obfuscatedValue, dummyBreakdownData(breakdownRow.dataKey)) should be(true)
        }
      }
    }
  }

  private def doQuery(outputTypes: Set[ResultOutputType])(i2b2XmlToReturn: => String): BaseShrineResponse = {
    doQuery(outputTypes, dao, MockHttpClient(i2b2XmlToReturn))
  }

  private def doQuery(outputTypes: Set[ResultOutputType], adapterDao: AdapterDao, httpClient: HttpClient): BaseShrineResponse = {
    val translator = new QueryDefinitionTranslator(new ExpressionTranslator(Map("foo" -> Set("bar"))))

    //NB: Don't obfuscate, for simpler testing
    val adapter = RunQueryAdapter(
      poster = Poster("crc-url", httpClient),
      dao = adapterDao,
      hiveCredentials = hiveCredentials,
      conceptTranslator = translator,
      adapterLockoutAttemptsThreshold = adapterLockoutThreshold,
      doObfuscation = false,
      runQueriesImmediately = true,
      breakdownTypes = DefaultBreakdownResultOutputTypes.toSet,
      collectAdapterAudit = false,
      botCountTimeThresholds = Seq.empty,
      obfuscator = Obfuscator(1,1.3,3)
    )

    import scala.concurrent.duration._

    val req = RunQueryRequest(projectId, 1.second, authn, expectedNetworkQueryId, Option(topicId), Option(topicName), outputTypes, queryDef)

    val networkAuthn = AuthenticationInfo("some-domain", "username", Credential("sadasdasdasd", false))

    val broadcastMessage = BroadcastMessage(queryId, networkAuthn, req)

    adapter.processRequest(broadcastMessage)
  }
}