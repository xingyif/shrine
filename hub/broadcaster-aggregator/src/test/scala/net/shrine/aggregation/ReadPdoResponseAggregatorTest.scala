package net.shrine.aggregation

import org.junit.Test
import org.junit.Assert.assertTrue
import net.shrine.protocol._
import collection.mutable.ListBuffer
import net.shrine.util.XmlDateHelper
import net.shrine.protocol.Result
import net.shrine.protocol.NodeId
import net.shrine.util.ShouldMatchersForJUnit

final class ReadPdoResponseAggregatorTest extends ShouldMatchersForJUnit {
  val aggregator = new ReadPdoResponseAggregator

  def createPdoResponse: ReadPdoResponse = {
    val patientId1 = "1000000001"
    val patientId2 = "1000000002"
    val patient1Param1 = ParamResponse("vital_status_cd", "vital_status_cd", "N")
    val patient1Param2 = ParamResponse("birth_date", "birth_date", "1985-11-17T00:00:00.000-05:00")
    val patient2Param1 = ParamResponse("vital_status_cd", "vital_status_cd", "N")
    val patient2Param2 = ParamResponse("birth_date", "birth_date", "1966-08-29T00:00:00.000-04:00")
    val patient1 = PatientResponse(patientId1, List(patient1Param1, patient1Param2))
    val patient2 = PatientResponse(patientId2, List(patient2Param1, patient2Param2))
    
    val now = XmlDateHelper.now
    
    val event1 = EventResponse(789012.toString, patientId1,
      Some(now),
      Some(now),
      Seq(patient1Param1, patient1Param2))
      
    val event2 = EventResponse(123456.toString, patientId2, None, None,
      Seq(patient1Param1, patient1Param2))
      
    val observationEvent1 = <event_id>2005000001</event_id>
    val observationEvent2 = <event_id>2005000002</event_id>
      
    val observation1 = ObservationResponse(None, "eventId", None, "patientId", None, None, None, "observerCode", "startDate", None, "valueTypeCode",None,None,None,None,None,None,None, Seq(ParamResponse("someParam1", "someColumn1", "someValue1")))
    val observation2 = ObservationResponse(None, "eventId", None, "patientId", None, None, None, "observerCode", "startDate", None, "valueTypeCode",None,None,None,None,None,None,None, Seq(ParamResponse("someParam2", "someColumn2", "someValue2")))
    
    ReadPdoResponse(Seq(event1, event2), Seq(patient1, patient2), Seq(observation1, observation2))
  }

  @Test
  def testAggregate {
    import scala.concurrent.duration._
    
    val result1 = Result(NodeId("A"), 1.second, createPdoResponse)
    val result2 = Result(NodeId("B"), 1.second, createPdoResponse)
    val result3 = Result(NodeId("C"), 1.second, createPdoResponse)
    
    val userId = "userId"
    
      val authn = AuthenticationInfo("domain", userId, Credential("value", false))
    
    //TODO: test handling passed-in errors
    val actual = aggregator.aggregate(Vector(result1, result2, result3), Nil).asInstanceOf[ReadPdoResponse]
    
    actual.patients.size should equal(6)

    val paramList = actual.patients.flatMap { p =>
      p.params.size should equal(2)
      
      p.params
    }

    paramList.filter(_.name == "vital_status_cd").size should equal(6)

    paramList.filter(_.name == "birth_date").size should equal(6)

    actual.patients.map(_.patientId).size should equal(6)

    actual.observations.size should equal(6)

    actual.events.size should equal(6)
  }
}