package net.shrine.protocol

import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author Justin Quan
 * @date 8/21/11
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final class ObservationResponseTest extends ShouldMatchersForJUnit {

  val xml = {
    <observation>
      <event_id>2005000001</event_id>
      <patient_id>1000000001</patient_id>
      <concept_cd>DEM|SEX:f</concept_cd>
      <observer_cd>@</observer_cd>
      <start_date>1985-11-17T00:00:00.000-05:00</start_date>
      <modifier_cd>@</modifier_cd>
      <valuetype_cd>@</valuetype_cd>
      <tval_char/>
      <nval_num/>
      <valueflag_cd/>
      <units_cd>@</units_cd>
      <end_date>2007-04-25T00:00:00.000-04:00</end_date>
      <location_cd>@</location_cd>
      <param name="someParam" column="someColumn"/>
    </observation>
  }

  val obs = ObservationResponse.fromI2b2(xml)

  @Test
  def testFromI2b2 {

    obs.params.size should equal (1)
    obs.params.apply(0).name should equal("someParam")
    obs.params.apply(0).column should equal("someColumn")
    obs.params.apply(0).value should equal("")

    obs.eventId should equal("2005000001")
    obs.patientId should equal("1000000001")
    obs.conceptCode.get should equal("DEM|SEX:f")
    obs.observerCode should equal("@")
    obs.startDate should equal("1985-11-17T00:00:00.000-05:00")
    obs.modifierCode should equal(Some("@"))
    obs.valueTypeCode should equal("@")
    obs.tvalChar should equal(None)
    obs.nvalNum should  equal(None)
    obs.valueFlagCode should equal(None)
    obs.unitsCode should equal(Some("@"))
    obs.endDate should equal(Some("2007-04-25T00:00:00.000-04:00"))
    obs.locationCode should equal(Some("@"))
  }

  @Test
  def testToFromI2B2 {
    val serialized: ObservationResponse = ObservationResponse.fromI2b2(obs.toI2b2)
    obs.params should equal(serialized.params)
    //obs.observation should equal(serialized.observation)
  }

  @Test
  def testToFromXml {
    val serialized: ObservationResponse = ObservationResponse.fromXml(obs.toXml)
    obs.params should equal(serialized.params)
    //obs.observation should equal(serialized.observation)
  }

  @Test
  def testHashCode {
    val obs1 = ObservationResponse.fromI2b2(xml)
    val obs2 = ObservationResponse.fromI2b2(xml)

    //hashcodes should be consistent
    obs1.hashCode should equal(obs1.hashCode)
    obs1.hashCode should equal(obs1.hashCode)

    obs2.hashCode should equal(obs2.hashCode)
    obs2.hashCode should equal(obs2.hashCode)

    //identical responses should have the same hashcode
    obs1 should equal(obs2)
    obs1.hashCode should equal(obs2.hashCode)

    //mutating any field should make for different hashcodes

    obs1.copy(tvalChar = Some("test")).hashCode should not equal (obs1.hashCode)

    obs1.copy(params = Seq.empty).hashCode should not equal (obs1.hashCode)
    obs1.copy(params = obs1.params :+ new ParamResponse("foo", "bar", "baz")).hashCode should not equal (obs1.hashCode)
  }

  @Test
  def testEquals {
    val obs0 = ObservationResponse.fromI2b2(xml)
    val obs1 = ObservationResponse.fromI2b2(xml)
    val obs2 = ObservationResponse.fromI2b2(xml)
    val obs3 = obs1.copy(startDate = "TEST")
    val obs4 = obs1.copy(params = Seq.empty)

    Seq(obs0, obs1, obs2, obs3, obs4).foreach { o =>
      //non-null things don't equal null
      o should not equal (null)

      //equals should be reflexive
      o should equal(o)
    }

    //equals should be symmetric
    obs1 should equal(obs2)
    obs2 should equal(obs1)

    //equals should be transitive
    obs0 should equal(obs1)
    obs1 should equal(obs2)
    obs0 should equal(obs2)

    //equals should be correct
    obs3 should not equal (obs4)
    obs4 should not equal (obs3)

    Seq(obs0, obs1, obs2).foreach { o =>
       o should not equal (obs3)
       o should not equal (obs4)

      obs3 should not equal (o)
      obs4 should not equal (o)
    }
  }
  
  @Test
  def testCanEqual {
    val obs1 = ObservationResponse.fromI2b2(xml)
    val obs2 = ObservationResponse.fromI2b2(xml)
    
    obs1.canEqual(obs2) should equal(true)
    obs2.canEqual(obs1) should equal(true)

    obs1.canEqual(null) should equal(false)
    obs1.canEqual("") should equal(false)
    obs1.canEqual(123) should equal(false)
    obs1.canEqual(<foo/>) should equal(false)
  }
}