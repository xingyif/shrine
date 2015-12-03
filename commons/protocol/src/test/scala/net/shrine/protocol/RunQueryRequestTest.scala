package net.shrine.protocol

import org.junit.Test
import xml.Utility
import scala.xml.XML
import net.shrine.util.XmlUtil
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.OccuranceLimited
import net.shrine.protocol.query.Term

/**
 * @author Bill Simons
 * @since 3/17/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
final class RunQueryRequestTest extends ShrineRequestValidator {
  private val queryId = 98765L
  private val topicId = "1"
  private val topicName = "Test Topic"
  private val queryDefinition = QueryDefinition("Ostium secundum@14:01:35", Term("""\\SHRINE\SHRINE\Diagnoses\Congenital anomalies\Cardiac and circulatory congenital anomalies\Atrial septal defect\Ostium secundum type atrial septal defect\"""))

  private val resultOutputTypes = {
    import ResultOutputType._

    (Seq(PATIENTSET, PATIENT_COUNT_XML) ++ DefaultBreakdownResultOutputTypes.values).sortBy(_.name).zipWithIndex.map {
      case (rot, i) =>
        rot.withId(i + 1)
    }
  }

  private val nonCountResultOutputTypes = resultOutputTypes.filterNot(_ == ResultOutputType.PATIENT_COUNT_XML)

  //add weird casing to make sure the code isn't case senstive, the client will send all sorts of weirdness
  private val resultOutputTypesI2b2Xml = XmlUtil.stripWhitespace {
    <result_output_list>
      <result_output priority_index="1" name="PatiEntSet"/>
      <result_output priority_index="2" name="patIent_Count_Xml"/>
      <result_output priority_index="3" name="patIent_gender_Count_Xml"/>
      <result_output priority_index="4" name="patIent_AgE_Count_Xml"/>
      <result_output priority_index="5" name="patIent_raCE_Count_Xml"/>
      <result_output priority_index="6" name="patIent_vITalSTaTuS_Count_Xml"/>
    </result_output_list>
  }

  override def messageBody = XmlUtil.stripWhitespace {
    <message_body>
      <ns4:psmheader>
        <user group={ domain } login={ username }>{ username }</user>
        <patient_set_limit>0</patient_set_limit>
        <estimated_time>0</estimated_time>
        <request_type>CRC_QRY_runQueryInstance_fromQueryDefinition</request_type>
      </ns4:psmheader>
      <ns4:request xsi:type="ns4:query_definition_requestType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        { queryDefinition.toI2b2 }
        { resultOutputTypesI2b2Xml }
      </ns4:request>
      <shrine><queryTopicID>{ topicId }</queryTopicID></shrine>
    </message_body>
  }

  private def messageBodyNoTopicId = XmlUtil.stripWhitespace {
    <message_body>
      <ns4:psmheader>
        <user group={ domain } login={ username }>{ username }</user>
        <patient_set_limit>0</patient_set_limit>
        <estimated_time>0</estimated_time>
        <request_type>CRC_QRY_runQueryInstance_fromQueryDefinition</request_type>
      </ns4:psmheader>
      <ns4:request xsi:type="ns4:query_definition_requestType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        { queryDefinition.toI2b2 }
        { resultOutputTypesI2b2Xml }
      </ns4:request>
      <shrine></shrine>
    </message_body>
  }

  private def messageBodyNoOutputTypes = XmlUtil.stripWhitespace {
    <message_body>
      <ns4:psmheader>
        <user group={ domain } login={ username }>{ username }</user>
        <patient_set_limit>0</patient_set_limit>
        <estimated_time>0</estimated_time>
        <request_type>CRC_QRY_runQueryInstance_fromQueryDefinition</request_type>
      </ns4:psmheader>
      <ns4:request xsi:type="ns4:query_definition_requestType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        { queryDefinition.toI2b2 }
        <result_output_list></result_output_list>
      </ns4:request>
      <shrine><queryTopicID>{ topicId }</queryTopicID></shrine>
    </message_body>
  }

  private def messageBodyNoCountOutputType = XmlUtil.stripWhitespace {
    <message_body>
      <ns4:psmheader>
        <user group={ domain } login={ username }>{ username }</user>
        <patient_set_limit>0</patient_set_limit>
        <estimated_time>0</estimated_time>
        <request_type>CRC_QRY_runQueryInstance_fromQueryDefinition</request_type>
      </ns4:psmheader>
      <ns4:request xsi:type="ns4:query_definition_requestType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        { queryDefinition.toI2b2 }
        <result_output_list>
          <result_output priority_index="3" name="patIent_gender_Count_Xml"/>
          <result_output priority_index="4" name="patIent_AgE_Count_Xml"/>
          <result_output priority_index="5" name="patIent_raCE_Count_Xml"/>
          <result_output priority_index="6" name="patIent_vITalSTaTuS_Count_Xml"/>
        </result_output_list>
      </ns4:request>
      <shrine><queryTopicID>{ topicId }</queryTopicID></shrine>
    </message_body>
  }

  private val runQueryRequest = XmlUtil.stripWhitespace {
    <runQuery>
      { requestHeaderFragment }
      <queryId>{ queryId }</queryId>
      <topicId>{ topicId }</topicId>
      <topicName>{ topicName }</topicName>
      <outputTypes>
        { resultOutputTypes.map(_.toXml) }
      </outputTypes>
      { queryDefinition.toXml }
    </runQuery>
  }

  private val runQueryRequestNoCountOutputType = XmlUtil.stripWhitespace {
    <runQuery>
      { requestHeaderFragment }
      <queryId>{ queryId }</queryId>
      <topicId>{ topicId }</topicId>
      <outputTypes>
        { nonCountResultOutputTypes.map(_.toXml) }
      </outputTypes>
      { queryDefinition.toXml }
    </runQuery>
  }

  private val runQueryRequestNoOutputTypes = XmlUtil.stripWhitespace {
    <runQuery>
      { requestHeaderFragment }
      <queryId>{ queryId }</queryId>
      <topicId>{ topicId }</topicId>
      <outputTypes>
      </outputTypes>
      { queryDefinition.toXml }
    </runQuery>
  }

  private val runQueryRequestNoTopicId = XmlUtil.stripWhitespace {
    <runQuery>
      { requestHeaderFragment }
      <queryId>{ queryId }</queryId>
      <outputTypes>
        { resultOutputTypes.map(_.toXml) }
      </outputTypes>
      { queryDefinition.toXml }
    </runQuery>
  }

  @Test
  def testAddPatientCountXmlIfNecessary: Unit = {
    import scala.concurrent.duration._
    import ResultOutputType._
    import RunQueryRequest.addPatientCountXmlIfNecessary

    val breakdownOutputType = ResultOutputType("PATIENT_GENDER_COUNT_XML", true, I2b2Options("Number of patients by gender"), Some(99))

    val countReq = RunQueryRequest(
      "project-Id",
      1.minute,
      AuthenticationInfo("d", "u", Credential("p", false)),
      None,
      None,
      Set(PATIENT_COUNT_XML, breakdownOutputType),
      QueryDefinition("foo", Some(Term("bar"))))

    val nonCountReq = countReq.copy(outputTypes = countReq.outputTypes - PATIENT_COUNT_XML)

    val noOutputTypesReq = countReq.copy(outputTypes = Set.empty)

    countReq.outputTypes.contains(PATIENT_COUNT_XML) should be(true)
    nonCountReq.outputTypes.contains(PATIENT_COUNT_XML) should be(false)

    addPatientCountXmlIfNecessary(countReq) should equal(countReq)
    addPatientCountXmlIfNecessary(nonCountReq) should equal(countReq)

    addPatientCountXmlIfNecessary(noOutputTypesReq).outputTypes should equal(Set(PATIENT_COUNT_XML))
  }

  @Test
  def testElideAuthenticationInfo: Unit = {
    val req = RunQueryRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(request()).get

    req.authn.username should equal(username)
    req.authn.domain should equal(domain)
    req.authn.credential.value should equal(passwd)

    val elided = req.elideAuthenticationInfo

    elided.authn.username should equal("*******")
    elided.authn.domain should equal("*******")
    elided.authn.credential.value should equal("*******")
  }

  @Test
  override def testFromI2b2 {
    val translatedRequest = RunQueryRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(request()).get

    validateRequestWith(translatedRequest) {
      translatedRequest.topicId should equal(Some(topicId))
      translatedRequest.outputTypes should equal(ResultOutputType.nonErrorTypes.toSet ++ DefaultBreakdownResultOutputTypes.toSet)
      translatedRequest.queryDefinition should equal(queryDefinition)

      val queryDefNode = translatedRequest.queryDefinition.toI2b2

      queryDefNode.head.prefix should equal(queryDefNode.head.scope.getPrefix(RunQueryRequest.neededI2b2Namespace))
    }
  }

  @Test
  def testFromI2b2NoTopicId {
    val translatedRequest = RunQueryRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(request(() => messageBodyNoTopicId)).get

    validateRequestWith(translatedRequest) {
      translatedRequest.topicId should equal(None)
      translatedRequest.outputTypes should equal(ResultOutputType.nonErrorTypes.toSet ++ DefaultBreakdownResultOutputTypes.toSet)
      translatedRequest.queryDefinition should equal(queryDefinition)

      val queryDefNode = translatedRequest.queryDefinition.toI2b2

      queryDefNode.head.prefix should equal(queryDefNode.head.scope.getPrefix(RunQueryRequest.neededI2b2Namespace))
    }
  }

  @Test
  def testFromI2b2NoOutputTypes {
    val translatedRequest = RunQueryRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(request(() => messageBodyNoOutputTypes)).get

    validateRequestWith(translatedRequest) {
      translatedRequest.topicId should equal(Some(topicId))
      translatedRequest.outputTypes should equal(Set(ResultOutputType.PATIENT_COUNT_XML))
      translatedRequest.queryDefinition should equal(queryDefinition)

      val queryDefNode = translatedRequest.queryDefinition.toI2b2

      queryDefNode.head.prefix should equal(queryDefNode.head.scope.getPrefix(RunQueryRequest.neededI2b2Namespace))
    }
  }

  @Test
  def testFromI2b2NoCountOutputType {
    val translatedRequest = RunQueryRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(request(() => messageBodyNoCountOutputType)).get

    validateRequestWith(translatedRequest) {
      translatedRequest.topicId should equal(Some(topicId))
      translatedRequest.outputTypes should equal(DefaultBreakdownResultOutputTypes.toSet + ResultOutputType.PATIENT_COUNT_XML)
      translatedRequest.queryDefinition should equal(queryDefinition)

      val queryDefNode = translatedRequest.queryDefinition.toI2b2

      queryDefNode.head.prefix should equal(queryDefNode.head.scope.getPrefix(RunQueryRequest.neededI2b2Namespace))
    }
  }

  @Test
  def testMapQueryDefinition {
    val outputTypes = ResultOutputType.nonBreakdownTypes.toSet

    val req = new RunQueryRequest(projectId, waitTime, authn, queryId, Option(topicId), Option(topicName), outputTypes, queryDefinition)

    val bogusTerm = Term("sa;ldk;alskd")

    val mapped = req.mapQueryDefinition(_.transform(_ => bogusTerm))

    (mapped eq req) should not be (true)

    mapped should not equal (req)

    mapped.projectId should equal(projectId)
    mapped.waitTime should equal(waitTime)
    mapped.authn should equal(authn)
    mapped.topicId should equal(Option(topicId))
    mapped.outputTypes should equal(outputTypes)
    mapped.queryDefinition.name should equal(queryDefinition.name)
    mapped.queryDefinition.expr.get should equal(bogusTerm)
  }

  @Test
  override def testShrineRequestFromI2b2 {
    val shrineRequest = CrcRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(request()).get

    shrineRequest.isInstanceOf[RunQueryRequest] should be(true)
  }

  @Test
  def testDoubleDispatchingShrineRequestFromI2b2 {
    val shrineRequest = HandleableShrineRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(request()).get

    shrineRequest.isInstanceOf[RunQueryRequest] should be(true)
  }

  @Test
  def testShrineXmlRoundTrip: Unit = {
    def doTest(outputTypes: Set[ResultOutputType]) {
      val req = RunQueryRequest(
        projectId,
        waitTime,
        authn,
        queryId,
        Option(topicId),
        Option(topicName),
        outputTypes,
        queryDefinition)

      val roundTripped = RunQueryRequest.fromXml(Set.empty)(req.toXml).get

      roundTripped should equal(req)
    }

    doTest(ResultOutputType.nonErrorTypes.toSet)

    doTest(ResultOutputType.nonErrorTypes.toSet ++ DefaultBreakdownResultOutputTypes.toSet)
  }

  @Test
  override def testToXml {
    import scala.concurrent.duration._

    RunQueryRequest(
      projectId,
      waitTime,
      authn,
      queryId,
      Option(topicId),
      Option(topicName),
      resultOutputTypes.toSet,
      queryDefinition).toXml should equal(runQueryRequest)
  }

  @Test
  def testToXmlNoTopicId {
    RunQueryRequest(
      projectId,
      waitTime,
      authn,
      queryId,
      None,
      None,
      resultOutputTypes.toSet,
      queryDefinition).toXml should equal(runQueryRequestNoTopicId)
  }

  @Test
  override def testToI2b2 {
    val req = RunQueryRequest(
      projectId,
      waitTime,
      authn,
      queryId,
      Option(topicId),
      Option(topicName),
      resultOutputTypes.toSet,
      queryDefinition)

    val actual = RunQueryRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(req.toI2b2).get

    validateRequestWith(actual) {
      actual.topicId should equal(Some(topicId))
      actual.outputTypes should equal(resultOutputTypes.toSet)
      actual.queryDefinition should equal(queryDefinition)
    }
  }

  @Test
  def testToI2b2NoTopicId {
    val req = RunQueryRequest(
      projectId,
      waitTime,
      authn,
      queryId,
      None,
      None,
      resultOutputTypes.toSet,
      queryDefinition)

    val actual = RunQueryRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(req.toI2b2).get

    validateRequestWith(actual) {
      actual.topicId should equal(None)
      actual.outputTypes should equal(resultOutputTypes.toSet)
      actual.queryDefinition should equal(queryDefinition)
    }
  }

  @Test
  override def testFromXml {
    val actual = RunQueryRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet)(runQueryRequest).get

    validateRequestWith(actual) {
      actual.networkQueryId should equal(queryId)
      actual.topicId should equal(Some(topicId))
      actual.outputTypes should equal(resultOutputTypes.toSet)
      actual.queryDefinition should equal(queryDefinition)

      val queryDefNode = actual.queryDefinition.toXml.head

      queryDefNode.prefix should be(null)
      queryDefNode.namespace should be(null)
    }
  }

  @Test
  def testFromXmlNoTopicId {
    val actual = RunQueryRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet)(runQueryRequestNoTopicId).get

    validateRequestWith(actual) {
      actual.networkQueryId should equal(queryId)
      actual.topicId should equal(None)
      actual.outputTypes should equal(resultOutputTypes.toSet)
      actual.queryDefinition should equal(queryDefinition)

      val queryDefNode = actual.queryDefinition.toXml.head

      queryDefNode.prefix should be(null)
      queryDefNode.namespace should be(null)
    }
  }

  @Test
  def testFromXmlNoOutputTypes {
    val actual = RunQueryRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet)(runQueryRequestNoOutputTypes).get

    validateRequestWith(actual) {
      actual.networkQueryId should equal(queryId)
      actual.topicId should equal(Some(topicId))
      actual.outputTypes should equal(Set(ResultOutputType.PATIENT_COUNT_XML))
      actual.queryDefinition should equal(queryDefinition)

      val queryDefNode = actual.queryDefinition.toXml.head

      queryDefNode.prefix should be(null)
      queryDefNode.namespace should be(null)
    }
  }

  @Test
  def testFromXmlNoCountOutputType {
    val actual = RunQueryRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet)(runQueryRequestNoCountOutputType).get

    validateRequestWith(actual) {
      actual.networkQueryId should equal(queryId)
      actual.topicId should equal(Some(topicId))
      actual.outputTypes should equal(nonCountResultOutputTypes.toSet + ResultOutputType.PATIENT_COUNT_XML)
      actual.queryDefinition should equal(queryDefinition)

      val queryDefNode = actual.queryDefinition.toXml.head

      queryDefNode.prefix should be(null)
      queryDefNode.namespace should be(null)
    }
  }

  @Test
  def testShrineRequestFromXml {
    ShrineRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet)(runQueryRequest).get.isInstanceOf[RunQueryRequest] should be(true)
  }
}