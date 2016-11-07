package net.shrine.steward.db

import net.shrine.authorization.steward.{InboundShrineQuery, InboundTopicRequest, OutboundTopic, OutboundUser, QueryContents, ResearcherToAudit, TopicState}
import net.shrine.i2b2.protocol.pm.User
import net.shrine.protocol.Credential
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Suite}

import scala.concurrent.duration._

/**
  * Tests of data steward db functions not excercised via the StewardService
  *
  * @author david
  * @since 1.22
  */

@RunWith(classOf[JUnitRunner])
class StewardServiceTest extends FlatSpec with TestWithDatabase {

  val researcherUserName = "ben"
  val researcherFullName = "Ben here before"

  val researcherUser = User(
    fullName = researcherFullName,
    username = researcherUserName,
    domain = "domain",
    credential = new Credential("ben's password",false),
    params = Map(),
    rolesByProject = Map()
  )

  val researcherOutboundUser = OutboundUser.createFromUser(researcherUser)

  "The database" should "record an audit request" in {

    val now = System.currentTimeMillis()
    val expectedAuditRequests: Seq[ResearcherToAudit] = Seq(ResearcherToAudit(researcherOutboundUser,31,now-2000,now))
    val expectedAuditRecords = expectedAuditRequests.map(x => UserAuditRecord(x.researcher.userName,x.count,now))

    StewardDatabase.db.logAuditRequests(expectedAuditRequests,now)
    val actual: Seq[UserAuditRecord] = StewardDatabase.db.selectAllAuditRequests

    assertResult(expectedAuditRecords)(actual)
  }

  "The database" should "supply the most recent audit request" in {

    val now = System.currentTimeMillis()
    val firstAuditRecords: Seq[ResearcherToAudit] = Seq(ResearcherToAudit(researcherOutboundUser,31,now-5000,now-3000))
    val secondAuditRecords: Seq[ResearcherToAudit] = Seq(ResearcherToAudit(researcherOutboundUser,31,now-2000,now))

    val expectedAuditRecords = secondAuditRecords.map(x => UserAuditRecord(x.researcher.userName,x.count,now))

    StewardDatabase.db.logAuditRequests(firstAuditRecords,now)
    val actual: Seq[UserAuditRecord] = StewardDatabase.db.selectMostRecentAuditRequests

    assertResult(expectedAuditRecords)(actual)
  }

  val uncontroversialTopic = OutboundTopic(1,"UncontroversialKidneys","Study kidneys without controversy",researcherOutboundUser,0L,TopicState.pending.name,researcherOutboundUser,0L)
  val stewardUserName = "dave"
  val queryContent: QueryContents = "<queryDefinition><name>18-34 years old@18:31:51</name><expr><term>\\\\SHRINE\\SHRINE\\Demographics\\Age\\18-34 years old\\</term></expr></queryDefinition>"

  "The database" should "recommend an audit for a user who has never been audited and has an unaudited query far enough in the past" in {

    StewardDatabase.db.upsertUser(researcherUser)

    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))
    StewardDatabase.db.changeTopicState(1,TopicState.approved,stewardUserName)
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(0,"test query",queryContent))
    val now = System.currentTimeMillis()


    Thread.sleep(20)

    val audits: Seq[ResearcherToAudit] = StewardDatabase.db.selectResearchersToAudit(30,10 milliseconds,System.currentTimeMillis())

    val expected = Seq(ResearcherToAudit(researcherOutboundUser,1,now - 20,now))

    assertResult(expected.size)(audits.size)
    assertResult(true)(expected.zip(audits).forall(x => x._1.sameExceptForTimes(x._2)))
  }

  "The database" should "not recommend an audit for a user who has never been audited and has an unaudited query not far enough in the past" in {

    StewardDatabase.db.upsertUser(researcherUser)

    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))
    StewardDatabase.db.changeTopicState(1,TopicState.approved,stewardUserName)
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(0,"test query",queryContent))
    val now = System.currentTimeMillis()


    Thread.sleep(20)

    val audits: Seq[ResearcherToAudit] = StewardDatabase.db.selectResearchersToAudit(30,30 days,System.currentTimeMillis())

    assertResult(Seq.empty)(audits)
  }

  "The database" should "recommend an audit for a user who has been audited and has an unaudited query far enough in the past" in {

    val now = System.currentTimeMillis()

    StewardDatabase.db.upsertUser(researcherUser)

    val oldAuditRequests: Seq[ResearcherToAudit] = Seq(ResearcherToAudit(researcherOutboundUser,31,now-2000,now-1000))
    StewardDatabase.db.logAuditRequests(oldAuditRequests,now)

    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))
    StewardDatabase.db.changeTopicState(1,TopicState.approved,stewardUserName)
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(0,"test query",queryContent))

    Thread.sleep(20)

    val audits: Seq[ResearcherToAudit] = StewardDatabase.db.selectResearchersToAudit(30,10 milliseconds,System.currentTimeMillis())

    val expected = Seq(ResearcherToAudit(researcherOutboundUser,1,now - 20,now))

    assertResult(expected.size)(audits.size)
    assertResult(true)(expected.zip(audits).forall(x => x._1.sameExceptForTimes(x._2)))
  }

  "The database" should "not recommend an audit for a user who has been audited and has an unaudited query not far enough in the past" in {

    val now = System.currentTimeMillis()

    StewardDatabase.db.upsertUser(researcherUser)

    val oldAuditRequests: Seq[ResearcherToAudit] = Seq(ResearcherToAudit(researcherOutboundUser,31,now-2000,now-1000))
    StewardDatabase.db.logAuditRequests(oldAuditRequests,now)

    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))
    StewardDatabase.db.changeTopicState(1,TopicState.approved,stewardUserName)
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(0,"test query",queryContent))

    Thread.sleep(20)

    val audits: Seq[ResearcherToAudit] = StewardDatabase.db.selectResearchersToAudit(30,30 days,System.currentTimeMillis())

    assertResult(Seq.empty)(audits)
  }

  "The database" should "recommend an audit for a user who has never been audited and has run many queries" in {

    StewardDatabase.db.upsertUser(researcherUser)

    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))
    StewardDatabase.db.changeTopicState(1,TopicState.approved,stewardUserName)
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(0,"test query1",queryContent))
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(1,"test query1",queryContent))
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(2,"test query1",queryContent))
    val now = System.currentTimeMillis()

    val audits: Seq[ResearcherToAudit] = StewardDatabase.db.selectResearchersToAudit(3,30 days,System.currentTimeMillis())

    val expected = Seq(ResearcherToAudit(researcherOutboundUser,3,now - 20,now))

    assertResult(expected.size)(audits.size)
    assertResult(true)(expected.zip(audits).forall(x => x._1.sameExceptForTimes(x._2)))
  }

  "The database" should "not recommend an audit for a user who has never been audited and has not run many queries" in {

    StewardDatabase.db.upsertUser(researcherUser)

    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))
    StewardDatabase.db.changeTopicState(1,TopicState.approved,stewardUserName)
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(0,"test query1",queryContent))
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(1,"test query1",queryContent))
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(2,"test query1",queryContent))
    val now = System.currentTimeMillis()

    val audits: Seq[ResearcherToAudit] = StewardDatabase.db.selectResearchersToAudit(30,30 days,System.currentTimeMillis())

    assertResult(Seq.empty)(audits)
  }

  "The database" should "recommend an audit for a user who has been audited and has run many queries" in {

    val now = System.currentTimeMillis()

    StewardDatabase.db.upsertUser(researcherUser)

    val oldAuditRequests: Seq[ResearcherToAudit] = Seq(ResearcherToAudit(researcherOutboundUser,31,now-2000,now-1000))
    StewardDatabase.db.logAuditRequests(oldAuditRequests,now)

    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))
    StewardDatabase.db.changeTopicState(1,TopicState.approved,stewardUserName)
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(0,"test query1",queryContent))
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(1,"test query1",queryContent))
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(2,"test query1",queryContent))

    val audits: Seq[ResearcherToAudit] = StewardDatabase.db.selectResearchersToAudit(3,30 days,System.currentTimeMillis())

    val expected = Seq(ResearcherToAudit(researcherOutboundUser,3,now - 20,now))

    assertResult(expected.size)(audits.size)
    assertResult(true)(expected.zip(audits).forall(x => x._1.sameExceptForTimes(x._2)))
  }

  "The database" should "not recommend an audit for a user who has been audited and has not run many queries" in {

    val now = System.currentTimeMillis()

    StewardDatabase.db.upsertUser(researcherUser)

    val oldAuditRequests: Seq[ResearcherToAudit] = Seq(ResearcherToAudit(researcherOutboundUser,31,now-2000,now-1000))
    StewardDatabase.db.logAuditRequests(oldAuditRequests,now)

    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))
    StewardDatabase.db.changeTopicState(1,TopicState.approved,stewardUserName)
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(0,"test query1",queryContent))
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(1,"test query1",queryContent))
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(2,"test query1",queryContent))

    val audits: Seq[ResearcherToAudit] = StewardDatabase.db.selectResearchersToAudit(30,30 days,System.currentTimeMillis())

    val expected = Seq(ResearcherToAudit(researcherOutboundUser,3,now - 20,now))

    assertResult(Seq.empty)(audits)
  }
}

trait TestWithDatabase extends BeforeAndAfterEach { this:Suite =>

  override def beforeEach() = {
    StewardDatabase.db.createTables()
  }

  override def afterEach() = {
    StewardDatabase.db.nextTopicId.set(1)
    StewardDatabase.db.dropTables()
  }

}
