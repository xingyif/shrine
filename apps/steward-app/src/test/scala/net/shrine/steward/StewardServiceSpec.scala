package net.shrine.steward

import net.shrine.authorization.steward.{TopicsPerState, QueriesPerUser, InboundTopicRequest, InboundShrineQuery, QueryHistory, StewardsTopics, ResearchersTopics, OutboundShrineQuery, TopicState, OutboundUser, OutboundTopic, stewardRole}
import net.shrine.i2b2.protocol.pm.User
import net.shrine.protocol.Credential
import net.shrine.steward.db.{QueryParameters, UserRecord, StewardDatabase}
import org.json4s.native.JsonMethods.parse
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Suite, BeforeAndAfterEach, FlatSpec}
import spray.http.BasicHttpCredentials
import spray.routing.MalformedRequestContentRejection

import spray.testkit.ScalatestRouteTest
import spray.http.StatusCodes.{OK,UnavailableForLegalReasons,Accepted,Unauthorized,UnprocessableEntity,NotFound,Forbidden,PermanentRedirect,TemporaryRedirect}


@RunWith(classOf[JUnitRunner])
class StewardServiceTest extends FlatSpec with ScalatestRouteTest with TestWithDatabase with StewardService {
  def actorRefFactory = system

  import scala.concurrent.duration._
  implicit val routeTestTimeout = RouteTestTimeout(10 seconds)

  val researcherUserName = "ben"
  val researcherFullName = researcherUserName

  val stewardUserName = "dave"
  val stewardFullName = stewardUserName
  /**
   * to run these tests with I2B2
   * add a user named qep, to be the qep
   * add a Boolean parameter for qep, qep, true
   * add a user named ben, to be a researcher
   * add a user named dave, to be the data steward
   * add a Boolean parameter for dave, DataSteward, true
   * add all three users to the i2b2 project
   */
  val stewardCredentials = BasicHttpCredentials(stewardUserName,"kablam")
  val researcherCredentials = BasicHttpCredentials(researcherUserName,"kapow")
  val qepCredentials = BasicHttpCredentials("qep","trustme")
  val badCredentials = BasicHttpCredentials("qep","wrongPassword")

  val researcherUser = User(
    fullName = researcherUserName,
    username = researcherFullName,
    domain = "domain",
    credential = new Credential("ben's password",false),
    params = Map(),
    rolesByProject = Map()
  )

  val stewardUser = User(
    fullName = stewardUserName,
    username = stewardFullName,
    domain = "domain",
    credential = new Credential("dave's password",false),
    params = Map(stewardRole -> "true"),
    rolesByProject = Map()
  )

  val researcherOutboundUser = OutboundUser.createFromUser(researcherUser)
  val stewardOutboundUser = OutboundUser.createFromUser(stewardUser)

  val uncontroversialTopic = OutboundTopic(1,"UncontroversialKidneys","Study kidneys without controversy",researcherOutboundUser,0L,TopicState.pending.name,researcherOutboundUser,0L)
  val forbiddenTopicId = 0

  "StewardService" should  "return an OK and the correct createTopicsMode name" in {

    StewardConfigSource.configForBlock(StewardConfigSource.createTopicsModeConfigKey,CreateTopicsMode.Pending.name,s"${CreateTopicsMode.Pending.name} test") {
      {
        Get(s"/about/createTopicsMode") ~>
          route ~> check {

          assertResult(OK)(status)
          val createTopicsModeName = new String(body.data.toByteArray)

          assertResult(s""""${StewardConfigSource.createTopicsInState.name}"""")(createTopicsModeName)
          assertResult(s""""${CreateTopicsMode.Pending.name}"""")(createTopicsModeName)
        }
      }
    }
    StewardConfigSource.configForBlock(StewardConfigSource.createTopicsModeConfigKey,CreateTopicsMode.Approved.name,s"${CreateTopicsMode.Approved.name} test") {
      Get(s"/about/createTopicsMode") ~>
        route ~> check {

        assertResult(OK)(status)
        val createTopicsModeName = new String(body.data.toByteArray)

        assertResult( s""""${StewardConfigSource.createTopicsInState.name}"""")(createTopicsModeName)
        assertResult( s""""${CreateTopicsMode.Approved.name}"""")(createTopicsModeName)
      }
    }
    StewardConfigSource.configForBlock(StewardConfigSource.createTopicsModeConfigKey,CreateTopicsMode.TopicsIgnoredJustLog.name,s"${CreateTopicsMode.TopicsIgnoredJustLog.name} test") {
      Get(s"/about/createTopicsMode") ~>
        route ~> check {

        assertResult(OK)(status)
        val createTopicsModeName = new String(body.data.toByteArray)

        assertResult( s""""${StewardConfigSource.createTopicsInState.name}"""")(createTopicsModeName)
        assertResult( s""""${CreateTopicsMode.TopicsIgnoredJustLog.name}"""")(createTopicsModeName)
      }
    }
  }

  "StewardService" should  "return an OK and a valid outbound user for a user/whoami request" in {

      Get(s"/user/whoami") ~>
        addCredentials(researcherCredentials) ~>
        route ~> check {

        assertResult(OK)(status)
        val userJson = new String(body.data.toByteArray)
        val outboundUser = parse(userJson).extract[OutboundUser]
        assertResult(researcherOutboundUser)(outboundUser)
      }
    }

  "StewardService" should  "return a 200 for a user/whoami request with bad credentials, with a body of 'AuthenticationFailed'" in {
    //    "StewardService" should  "return an TemporaryRedirect for a user/whoami request with bad credentials" in {

    Get(s"/user/whoami") ~>
      addCredentials(badCredentials) ~>
      sealRoute(route) ~> check {

      assertResult(OK)(status)
      assertResult(""""AuthenticationFailed"""")(new String(body.data.toByteArray))
    }
  }
/* todo
  "StewardService" should  "return a 200 for a user/whoami request with bad credentials, with a body of 'AuthenticationFailed' even wiht a back-tick" in {
    //    "StewardService" should  "return an TemporaryRedirect for a user/whoami request with bad credentials" in {

    val badCredentials = BasicHttpCredentials("o`brien","wrongPassword")

    Get(s"/user/whoami") ~>
      addCredentials(badCredentials) ~>
      sealRoute(route) ~> check {

      assertResult(OK)(status)
      assertResult(""""AuthenticationFailed"""")(new String(body.data.toByteArray))
    }
  }
*/
  "StewardService" should  "return an OK for an approved request" in {

      StewardDatabase.db.upsertUser(researcherUser)
      StewardDatabase.db.upsertUser(stewardUser)

      val topicInDb = StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))

      val dbFound = StewardDatabase.db.changeTopicState(uncontroversialTopic.id,TopicState.approved,stewardUserName)

      Post(s"/qep/requestQueryAccess/user/${researcherUserName}/topic/${uncontroversialTopic.id}",InboundShrineQuery(1,"test query","crazy syntax")) ~>
        addCredentials(qepCredentials) ~>
        route ~> check {
          assertResult(OK)(status)
      }
    }

    "StewardService" should  "complain about bad http credentials from the QEP" in {
      Post(s"/qep/requestQueryAccess/user/${researcherUserName}/topic/${uncontroversialTopic.id}",InboundShrineQuery(2,"test query","too bad about your password")) ~>
        addCredentials(badCredentials) ~>
        sealRoute(route) ~> check {
          assertResult(Unauthorized)(status)
      }
    }

    "StewardService" should  "complain about unexpected json" in {
      Post(s"/qep/requestQueryAccess/user/${researcherUserName}/topic/${uncontroversialTopic.id}","""{"field":"not in ShrineQuery"}""")  ~>
        addCredentials(qepCredentials) ~>
        route ~> check {
          assertResult(false)(handled)
          assertResult(true)(rejection.isInstanceOf[MalformedRequestContentRejection])
      }
    }

    "StewardService" should "return a rejection for an unacceptable request" in {

      StewardDatabase.db.upsertUser(researcherUser)
      StewardDatabase.db.upsertUser(stewardUser)
      StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))
      StewardDatabase.db.changeTopicState(1,TopicState.rejected,stewardUserName)

      Post(s"/qep/requestQueryAccess/user/${researcherUserName}/topic/${uncontroversialTopic.id}",InboundShrineQuery(3,"test query","too bad about your topic")) ~>
        addCredentials(qepCredentials) ~>
        route ~> check {
        assertResult(UnavailableForLegalReasons)(status)
      }
    }

    "StewardService" should "return a rejection for a pending topic" in {

      StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))

      Post(s"/qep/requestQueryAccess/user/${researcherUserName}/topic/${uncontroversialTopic.id}",InboundShrineQuery(4,"test query","topic still pending")) ~>
        addCredentials(qepCredentials) ~>
        route ~> check {
        assertResult(UnavailableForLegalReasons)(status)
      }
    }

    "StewardService" should "return an UnprocessableEntity for an unknown topic" in {

      Post(s"/qep/requestQueryAccess/user/${researcherUserName}/topic/${forbiddenTopicId}",InboundShrineQuery(5,"test query","no one knows about your topic")) ~>
        addCredentials(qepCredentials) ~>
        route ~> check {
        assertResult(UnprocessableEntity)(status)
      }
    }

  "StewardService" should " return an UnprocessableEntity for query requests with no topic" in {

    Post(s"/qep/requestQueryAccess/user/${researcherUserName}",InboundShrineQuery(5,"test query","Not even using a topic")) ~>
      addCredentials(qepCredentials) ~>
      route ~> check {
      assertResult(UnprocessableEntity)(status)
    }
  }

  "StewardService" should " accept query requests with no topic in 'just log and approve everything' mode " in {


    StewardConfigSource.configForBlock(StewardConfigSource.createTopicsModeConfigKey,CreateTopicsMode.TopicsIgnoredJustLog.name,s"${CreateTopicsMode.TopicsIgnoredJustLog.name} test") {
      Post(s"/qep/requestQueryAccess/user/${researcherUserName}", InboundShrineQuery(5, "test query", "Not even using a topic")) ~>
        addCredentials(qepCredentials) ~>
        route ~> check {
        assertResult(OK)(status)
      }
    }
  }

  "StewardService" should "return approved topics for the qep" in {

      StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))

      Get(s"/qep/approvedTopics/user/${researcherUserName}") ~>
        addCredentials(qepCredentials) ~>
        route ~> check {
          status === OK

          val topicsJson = new String(body.data.toByteArray)
          val topics = parse(topicsJson).extract[ResearchersTopics]

          ResearchersTopics(researcherUserName,1,0,Seq(uncontroversialTopic)).sameExceptForTimes(topics) === true
        }
    }


    "StewardService" should "return the list of a researcher's Topics as Json" in {

      StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))

      Get(s"/researcher/topics") ~>
        addCredentials(researcherCredentials) ~>
        route ~> check {

          assertResult(OK)(status)

          val topicsJson = new String(body.data.toByteArray)
          val topics = parse(topicsJson).extract[ResearchersTopics]

          assertResult(true)(ResearchersTopics(researcherUserName,1,0,Seq(uncontroversialTopic)).sameExceptForTimes(topics))
      }
    }

    "StewardService" should "return the list of a researcher's Topics as Json for various states" in {

      StewardDatabase.db.upsertUser(researcherUser)
      StewardDatabase.db.upsertUser(stewardUser)
      StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))

      Get(s"/researcher/topics?state=Pending") ~>
        addCredentials(researcherCredentials) ~>
        route ~> check {

          val topicsJson = new String(body.data.toByteArray)
          val topics = parse(topicsJson).extract[ResearchersTopics]

          assertResult(true)(ResearchersTopics(researcherUserName,1,0,Seq(uncontroversialTopic)).sameExceptForTimes(topics))

          assertResult(OK)(status)
      }

      StewardDatabase.db.changeTopicState(1,TopicState.approved,stewardUserName)

      Get(s"/researcher/topics?state=Approved") ~>
        addCredentials(researcherCredentials) ~>
        route ~> check {

          val topicsJson = new String(body.data.toByteArray)
          val topics = parse(topicsJson).extract[ResearchersTopics]

          assertResult(true)(ResearchersTopics(researcherUserName,1,0,Seq(
            OutboundTopic(1,uncontroversialTopic.name,uncontroversialTopic.description,researcherOutboundUser,0,TopicState.approved.name,stewardOutboundUser,0))).sameExceptForTimes(topics))

          assertResult(OK)(status)
      }

      StewardDatabase.db.changeTopicState(1,TopicState.rejected,stewardUserName)

      Get(s"/researcher/topics?state=Rejected") ~>
        addCredentials(researcherCredentials) ~>
        route ~> check {

          val topicsJson = new String(body.data.toByteArray)
          val topics = parse(topicsJson).extract[ResearchersTopics]

          assertResult(true)(ResearchersTopics(researcherUserName,1,0,Seq(
            OutboundTopic(1,uncontroversialTopic.name,uncontroversialTopic.description,researcherOutboundUser,0,TopicState.rejected.name,stewardOutboundUser,0))).sameExceptForTimes(topics))

          assertResult(OK)(status)
      }
    }

    "DataStewardService" should "reject nonsense topic request states" in {
      Get(s"/researcher/topics?state=nonsense") ~>
        addCredentials(researcherCredentials) ~>
        route ~> check {

        assertResult(UnprocessableEntity)(status)
      }
    }

    "StewardService" should "return the list of a researcher's Topics as Json with skip and limit set" in {

      StewardDatabase.db.upsertUser(researcherUser)
      StewardDatabase.db.upsertUser(stewardUser)
      createFiveTopics()

      Get(s"/researcher/topics?skip=0&limit=2") ~>
        addCredentials(researcherCredentials) ~>
        route ~> check {
          val topicsJson = new String(body.data.toByteArray)
          val topics = parse(topicsJson).extract[ResearchersTopics]

          assertResult(2)(topics.topics.size)
          assertResult(OK)(status)
      }

      Get(s"/researcher/topics?skip=2&limit=2") ~>
        addCredentials(researcherCredentials) ~>
        route ~> check {
          val topicsJson = new String(body.data.toByteArray)
          val topics = parse(topicsJson).extract[ResearchersTopics]

          assertResult(2)(topics.topics.size)
          assertResult(OK)(status)
      }

      Get(s"/researcher/topics?skip=4&limit=2") ~>
        addCredentials(researcherCredentials) ~>
        route ~> check {
        val topicsJson = new String(body.data.toByteArray)
        val topics = parse(topicsJson).extract[ResearchersTopics]

        assertResult(1)(topics.topics.size)
        assertResult(OK)(status)
      }
    }

    "StewardService" should "return the list of a researcher's Topics as Json with different sorting and ordering options" in {

      StewardDatabase.db.upsertUser(researcherUser)
      StewardDatabase.db.upsertUser(stewardUser)
      createFiveTopics()

      Get(s"/researcher/topics?sortBy=id") ~>
        addCredentials(researcherCredentials) ~>
        route ~> check {
        val topicsJson = new String(body.data.toByteArray)
        val topics = parse(topicsJson).extract[ResearchersTopics]

        assertResult(5)(topics.topics.size)
        assertResult(OK)(status)

        assertResult(topics.topics.sortBy(_.id))(topics.topics)
      }

      Get(s"/researcher/topics?sortBy=name") ~>
        addCredentials(researcherCredentials) ~>
        route ~> check {
        val topicsJson = new String(body.data.toByteArray)
        val topics = parse(topicsJson).extract[ResearchersTopics]

        assertResult(5)(topics.topics.size)
        assertResult(OK)(status)
        assertResult(topics.topics.sortBy(_.createdBy.userName))(topics.topics)
      }

      Get(s"/researcher/topics?sortBy=name&sortDirection=descending") ~>
        addCredentials(researcherCredentials) ~>
        route ~> check {
        val topicsJson = new String(body.data.toByteArray)
        val topics = parse(topicsJson).extract[ResearchersTopics]

        assertResult(5)(topics.topics.size)
        assertResult(OK)(status)
        assertResult(topics.topics.sortBy(_.name).reverse)(topics.topics)
      }

      Get(s"/researcher/topics?sortBy=name&sortDirection=ascending") ~>
        addCredentials(researcherCredentials) ~>
        route ~> check {
        val topicsJson = new String(body.data.toByteArray)
        val topics = parse(topicsJson).extract[ResearchersTopics]

        assertResult(5)(topics.topics.size)
        assertResult(OK)(status)
        assertResult(topics.topics.sortBy(_.createdBy.userName))(topics.topics)
      }
    }

  "StewardService" should "return the list of a researcher's query history as Json" in {

      StewardDatabase.db.upsertUser(researcherUser)
      StewardDatabase.db.upsertUser(stewardUser)

      StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))
      StewardDatabase.db.changeTopicState(1,TopicState.approved,stewardUserName)
      StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(0,"test query","Can we get it back?"))

      Get(s"/researcher/queryHistory") ~>
        addCredentials(researcherCredentials) ~> route ~> check {
        assertResult(OK)(status)

        val queriesJson = new String(body.data.toByteArray)
        val queries = parse(queriesJson).extract[QueryHistory]

        assertResult(List.empty)(QueryHistory(1,0,List(
          OutboundShrineQuery(1,0,"test query",researcherOutboundUser,Some(
            OutboundTopic(1,uncontroversialTopic.name,uncontroversialTopic.description,researcherOutboundUser,0,TopicState.approved.name,stewardOutboundUser,0)
          ),"Can we get it back?",TopicState.approved.name,0)
        )).differencesExceptTimes(queries))
      }
    }

    "StewardService" should "return the list of a researcher's query history as Json, filtered by state" in {

      StewardDatabase.db.upsertUser(researcherUser)
      StewardDatabase.db.upsertUser(stewardUser)
      StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))
      StewardDatabase.db.changeTopicState(1,TopicState.approved,stewardUserName)

      StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest("Forbidden topic","No way is the data steward going for this"))
      StewardDatabase.db.changeTopicState(2,TopicState.rejected,stewardUserName)

      StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(0,"test query","Can we get it back?"))
      StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(2),InboundShrineQuery(1,"forbidden query","Can we get it back?"))

      Get(s"/researcher/queryHistory?state=Approved") ~>
        addCredentials(researcherCredentials) ~> route ~> check {
        assertResult(OK)(status)

        val queriesJson = new String(body.data.toByteArray)
        val queries = parse(queriesJson).extract[QueryHistory]

        assertResult(List.empty)(QueryHistory(1,0,List(OutboundShrineQuery(1,0,"test query",researcherOutboundUser,Some(OutboundTopic(1,uncontroversialTopic.name,uncontroversialTopic.description,researcherOutboundUser,0,TopicState.approved.name,stewardOutboundUser,0)),"Can we get it back?",TopicState.approved.name,0))).differencesExceptTimes(queries))
      }
    }

    "StewardService" should "return the list of a researcher's query history as Json, with skip and limit parameters" in {

      StewardDatabase.db.upsertUser(researcherUser)
      StewardDatabase.db.upsertUser(stewardUser)
      createSixQueries()

      Get(s"/researcher/queryHistory?") ~>
        addCredentials(researcherCredentials) ~> route ~> check {
        assertResult(OK)(status)

        val queriesJson = new String(body.data.toByteArray)
        val queries = parse(queriesJson).extract[QueryHistory]

        assertResult(6)(queries.queryRecords.size)
      }

      Get(s"/researcher/queryHistory?skip=0&limit=5") ~>
        addCredentials(researcherCredentials) ~> route ~> check {
        assertResult(OK)(status)

        val queriesJson = new String(body.data.toByteArray)
        val queries = parse(queriesJson).extract[QueryHistory]

        assertResult(5)(queries.queryRecords.size)
      }

      Get(s"/researcher/queryHistory?skip=1&limit=4") ~>
        addCredentials(researcherCredentials) ~> route ~> check {
        assertResult(OK)(status)

        val queriesJson = new String(body.data.toByteArray)
        val queries = parse(queriesJson).extract[QueryHistory]

        assertResult(4)(queries.queryRecords.size)
      }

      Get(s"/researcher/queryHistory?skip=4&limit=5") ~>
        addCredentials(researcherCredentials) ~> route ~> check {
        assertResult(OK)(status)

        val queriesJson = new String(body.data.toByteArray)
        val queries = parse(queriesJson).extract[QueryHistory]

        assertResult(2)(queries.queryRecords.size)
      }
    }

    "StewardService" should "return the list of a researcher's query history as Json, using parameters for sorting" in {

      StewardDatabase.db.upsertUser(researcherUser)
      StewardDatabase.db.upsertUser(stewardUser)
      createSixQueries()

      Get(s"/researcher/queryHistory?sortBy=externalId") ~>
        addCredentials(researcherCredentials) ~> route ~> check {
        assertResult(OK)(status)

        val queriesJson = new String(body.data.toByteArray)
        val queries = parse(queriesJson).extract[QueryHistory]

        assertResult(6)(queries.queryRecords.size)
        assertResult(queries.queryRecords.sortBy(_.externalId))(queries.queryRecords)
      }

      Get(s"/researcher/queryHistory?sortBy=externalId&sortDirection=ascending") ~>
        addCredentials(researcherCredentials) ~> route ~> check {
        assertResult(OK)(status)

        val queriesJson = new String(body.data.toByteArray)
        val queries = parse(queriesJson).extract[QueryHistory]

        assertResult(6)(queries.queryRecords.size)
        assertResult(queries.queryRecords.sortBy(_.externalId))(queries.queryRecords)
      }

      Get(s"/researcher/queryHistory?sortBy=externalId&sortDirection=descending") ~>
        addCredentials(researcherCredentials) ~> route ~> check {
        assertResult(OK)(status)

        val queriesJson = new String(body.data.toByteArray)
        val queries = parse(queriesJson).extract[QueryHistory]

        assertResult(6)(queries.queryRecords.size)
        assertResult(queries.queryRecords.sortBy(_.externalId).reverse)(queries.queryRecords)
      }

      Get(s"/researcher/queryHistory?sortBy=stewardResponse") ~>
        addCredentials(researcherCredentials) ~> route ~> check {
        assertResult(OK)(status)

        val queriesJson = new String(body.data.toByteArray)
        val queries = parse(queriesJson).extract[QueryHistory]

        assertResult(6)(queries.queryRecords.size)
        assertResult(queries.queryRecords.sortBy(_.stewardResponse))(queries.queryRecords)
      }

      Get(s"/researcher/queryHistory?sortBy=name") ~>
        addCredentials(researcherCredentials) ~> route ~> check {
        assertResult(OK)(status)

        val queriesJson = new String(body.data.toByteArray)
        val queries = parse(queriesJson).extract[QueryHistory]

        assertResult(6)(queries.queryRecords.size)
        assertResult(queries.queryRecords.sortBy(_.name))(queries.queryRecords)
      }
    }

  "StewardService" should "return the list of a researcher's query history, using parameters for sorting, skip, and limit" in {

    StewardDatabase.db.upsertUser(researcherUser)
    StewardDatabase.db.upsertUser(stewardUser)
    createSixQueries()

    val sixQueries:Seq[OutboundShrineQuery] = {
      Get(s"/researcher/queryHistory") ~>
        addCredentials(researcherCredentials) ~> route ~> check {
        assertResult(OK)(status)

        val queriesJson = new String(body.data.toByteArray)
        val queries:QueryHistory = parse(queriesJson).extract[QueryHistory]

        assertResult(6)(queries.queryRecords.size)
        assertResult(queries.queryRecords.sortBy(_.externalId))(queries.queryRecords)

        queries.queryRecords
      }
    }

    Get(s"/researcher/queryHistory?skip=0&limit=3&sortBy=name") ~>
      addCredentials(researcherCredentials) ~> route ~> check {
      assertResult(OK)(status)

      val queriesJson = new String(body.data.toByteArray)
      val queries = parse(queriesJson).extract[QueryHistory]

      assertResult(3)(queries.queryRecords.size)
      assertResult(queries.queryRecords)(sixQueries.sortBy(_.name).take(3))
    }

    Get(s"/researcher/queryHistory?skip=2&limit=3&sortBy=name") ~>
      addCredentials(researcherCredentials) ~> route ~> check {
      assertResult(OK)(status)

      val queriesJson = new String(body.data.toByteArray)
      val queries = parse(queriesJson).extract[QueryHistory]

      assertResult(3)(queries.queryRecords.size)
      assertResult(queries.queryRecords)(sixQueries.sortBy(_.name).drop(2).take(3))
    }

  }


  "StewardService" should "return the list of a researcher's query history as Json, with filtered by topic" in {

      StewardDatabase.db.upsertUser(researcherUser)
      StewardDatabase.db.upsertUser(stewardUser)
      createSixQueries()

      Get(s"/researcher/queryHistory") ~>
        addCredentials(researcherCredentials) ~> route ~> check {
        assertResult(OK)(status)

        val queriesJson = new String(body.data.toByteArray)
        val queries = parse(queriesJson).extract[QueryHistory]

        assertResult(6)(queries.queryRecords.size)
      }

      Get(s"/researcher/queryHistory/topic/1") ~>
        addCredentials(researcherCredentials) ~> route ~> check {
        assertResult(OK)(status)

        val queriesJson = new String(body.data.toByteArray)
        val queries = parse(queriesJson).extract[QueryHistory]

        assertResult(3)(queries.queryRecords.size)
      }

      Get(s"/researcher/queryHistory/topic/1?skip=1&limit=2") ~>
        addCredentials(researcherCredentials) ~> route ~> check {
        assertResult(OK)(status)

        val queriesJson = new String(body.data.toByteArray)
        val queries = parse(queriesJson).extract[QueryHistory]

        assertResult(2)(queries.queryRecords.size)
      }

      Get(s"/researcher/queryHistory/topic/2") ~>
        addCredentials(researcherCredentials) ~> route ~> check {
        assertResult(OK)(status)

        val queriesJson = new String(body.data.toByteArray)
        val queries = parse(queriesJson).extract[QueryHistory]

        assertResult(3)(queries.queryRecords.size)
      }
    }


  "StewardService" should "return the list of a researcher's query history as Json, filtered by date" in {


    StewardDatabase.db.upsertUser(researcherUser)
    StewardDatabase.db.upsertUser(stewardUser)

    val startTime = System.currentTimeMillis()
    Thread.sleep(10)

    createSixQueries()

    Thread.sleep(10)
    val finishTime = System.currentTimeMillis()

    Get(s"/researcher/queryHistory") ~>
      addCredentials(researcherCredentials) ~> route ~> check {
      assertResult(OK)(status)

      val queriesJson = new String(body.data.toByteArray)
      val queries = parse(queriesJson).extract[QueryHistory]

      assertResult(6)(queries.queryRecords.size)
    }

    Get(s"/researcher/queryHistory?minDate=$startTime&maxDate=$finishTime") ~>
      addCredentials(researcherCredentials) ~> route ~> check {
      assertResult(OK)(status)

      val queriesJson = new String(body.data.toByteArray)
      val queries = parse(queriesJson).extract[QueryHistory]

      assertResult(6)(queries.queryRecords.size)
    }

    Get(s"/researcher/queryHistory?minDate=$startTime&maxDate=$startTime") ~>
      addCredentials(researcherCredentials) ~> route ~> check {
      assertResult(OK)(status)

      val queriesJson = new String(body.data.toByteArray)
      val queries = parse(queriesJson).extract[QueryHistory]

      assertResult(0)(queries.queryRecords.size)
    }

    Get(s"/researcher/queryHistory?minDate=$finishTime&maxDate=$finishTime") ~>
      addCredentials(researcherCredentials) ~> route ~> check {
      assertResult(OK)(status)

      val queriesJson = new String(body.data.toByteArray)
      val queries = parse(queriesJson).extract[QueryHistory]

      assertResult(0)(queries.queryRecords.size)
    }

  }

    "StewardService" should "return the list of a researcher's query history as Json, even including an unknown topic id" in {

      StewardDatabase.db.upsertUser(researcherUser)
      StewardDatabase.db.upsertUser(stewardUser)

      StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(0,"test query for unknown topic","Can we get it back?"))

      Get(s"/researcher/queryHistory") ~>
        addCredentials(researcherCredentials) ~> route ~> check {
        assertResult(OK)(status)

        val queriesJson = new String(body.data.toByteArray)
        val queries = parse(queriesJson).extract[QueryHistory]

        assertResult(List.empty)(QueryHistory(1,0,List(OutboundShrineQuery(1,0,"test query for unknown topic",researcherOutboundUser,None,"Can we get it back?",TopicState.unknownForUser.name,0))).differencesExceptTimes(queries))
      }
    }

  "StewardService" should "return the list of a researcher's query history as Json, sorted by topic name" in {

    StewardDatabase.db.upsertUser(researcherUser)
    StewardDatabase.db.upsertUser(stewardUser)
    createSixQueries()

    Get(s"/researcher/queryHistory?sortBy=topicName") ~>
      addCredentials(researcherCredentials) ~> route ~> check {
      assertResult(OK)(status)

      val queriesJson = new String(body.data.toByteArray)
      val queries = parse(queriesJson).extract[QueryHistory]

      assertResult(6)(queries.queryRecords.size)
//      assertResult(queries.queryRecords.sortBy(_.externalId))(queries.queryRecords)
    }

  }


  "StewardService" should "return counts of topics, total and by state" in {

    StewardDatabase.db.upsertUser(researcherUser)
    StewardDatabase.db.upsertUser(stewardUser)
    createFiveTopics()

    Get(s"/steward/statistics/topicsPerState") ~>
      addCredentials(stewardCredentials) ~> route ~> check {
      assertResult(OK)(status)

      val statisticsJson = new String(body.data.toByteArray)
      val statistics = parse(statisticsJson).extract[TopicsPerState]

      assertResult(5)(statistics.total)

      assertResult(TopicsPerState(5,Seq((TopicState.pending.name,5))))(statistics)
    }
  }

  "StewardService" should "injest a researcher's request to study a topic" in {
    Post(s"/researcher/requestTopicAccess",InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description)) ~>
      addCredentials(researcherCredentials) ~>
      route ~> check {
        assertResult(Accepted)(status)
        assertResult(1)(StewardDatabase.db.selectUsers.size)
        assertResult(UserRecord(researcherUserName,researcherUserName,false))(StewardDatabase.db.selectUsers.head)

        val topics = StewardDatabase.db.selectTopics(QueryParameters())
        assertResult(1)(topics.size)

        val topic = topics.head
        assertResult(uncontroversialTopic.name)(topic.name)
        assertResult(uncontroversialTopic.description)(topic.description)
        assertResult(researcherUserName)(topic.createdBy)
        assertResult(TopicState.pending)(topic.state)

    }
  }

  "StewardService" should "injest a second request from a researcher request to study a new topic" in {

    val secondTopicName = "Liver"
    val secondTopicDescription = "Liver Study"

    StewardDatabase.db.upsertUser(researcherUser)

    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))
    Post(s"/researcher/requestTopicAccess",InboundTopicRequest(secondTopicName,secondTopicDescription)) ~>
      addCredentials(researcherCredentials) ~> route ~> check {

      assertResult(Accepted)(status)
      assertResult(1)(StewardDatabase.db.selectUsers.size)
      assertResult(UserRecord(researcherUserName,researcherUserName,false))(StewardDatabase.db.selectUsers.head)

      val topics = StewardDatabase.db.selectTopics(QueryParameters())
      assertResult(2)(topics.size)

      val firstTopic = topics(0)
      assertResult(uncontroversialTopic.name)(firstTopic.name)
      assertResult(uncontroversialTopic.description)(firstTopic.description)
      assertResult(researcherUserName)(firstTopic.createdBy)
      assertResult(TopicState.pending)(firstTopic.state)

      val secondTopic = topics(1)
      assertResult(secondTopicName)(secondTopic.name)
      assertResult(secondTopicDescription)(secondTopic.description)
      assertResult(researcherUserName)(secondTopic.createdBy)
      assertResult(TopicState.pending)(secondTopic.state)
    }
  }

  "StewardService" should " place new topics in the Approved state in auto-approve mode" in {

    StewardConfigSource.configForBlock(StewardConfigSource.createTopicsModeConfigKey,CreateTopicsMode.TopicsIgnoredJustLog.name,s"${CreateTopicsMode.TopicsIgnoredJustLog.name} test") {
      Post(s"/researcher/requestTopicAccess",InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description)) ~>
        addCredentials(researcherCredentials) ~>
        route ~> check {
          assertResult(Accepted)(status)
          assertResult(1)(StewardDatabase.db.selectUsers.size)
          assertResult(UserRecord(researcherUserName, researcherUserName, false))(StewardDatabase.db.selectUsers.head)

          val topics = StewardDatabase.db.selectTopics(QueryParameters())
          assertResult(1)(topics.size)

          val topic = topics.head
          assertResult(uncontroversialTopic.name)(topic.name)
          assertResult(uncontroversialTopic.description)(topic.description)
          assertResult(researcherUserName)(topic.createdBy)
          assertResult(TopicState.approved)(topic.state)
      }
    }
  }

  "StewardService" should "injest a researcher's request to edit a topic" in {
    StewardDatabase.db.upsertUser(researcherUser)
    StewardDatabase.db.upsertUser(stewardUser)
    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))

    val updatedDescription = "Really you should accept this"

    Post(s"/researcher/editTopicRequest/1",InboundTopicRequest(uncontroversialTopic.name,updatedDescription)) ~>
      addCredentials(researcherCredentials) ~>
      route ~> check {
      assertResult(Accepted)(status)

      val topics = StewardDatabase.db.selectTopics(QueryParameters())
      assertResult(1)(topics.size)

      val topic = topics.head
      assertResult(uncontroversialTopic.name)(topic.name)
      assertResult(updatedDescription)(topic.description)
      assertResult(researcherUserName)(topic.createdBy)
      assertResult(researcherUserName)(topic.changedBy)
      assertResult(TopicState.pending)(topic.state)

    }
  }

  "StewardService" should "reject a researcher's request to edit a topic that does not exist" in {

    StewardDatabase.db.upsertUser(researcherUser)
    StewardDatabase.db.upsertUser(stewardUser)

    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))

    val updatedDescription = "Really you should accept this"

    Post(s"/researcher/editTopicRequest/2",InboundTopicRequest(uncontroversialTopic.name,updatedDescription)) ~>
      addCredentials(researcherCredentials) ~>
      route ~> check {
      assertResult(NotFound)(status)

      val topics = StewardDatabase.db.selectTopics(QueryParameters())
      assertResult(1)(topics.size)

      val topic = topics.head
      assertResult(uncontroversialTopic.name)(topic.name)
      assertResult(uncontroversialTopic.description)(topic.description)
      assertResult(researcherUserName)(topic.createdBy)
      assertResult(researcherUserName)(topic.changedBy)
      assertResult(TopicState.pending)(topic.state)

    }
  }

  "StewardService" should "reject a researcher's request to edit an approved topic" in {

    StewardDatabase.db.upsertUser(researcherUser)
    StewardDatabase.db.upsertUser(stewardUser)

    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))
    StewardDatabase.db.changeTopicState(1,TopicState.approved,stewardUserName)

    val updatedDescription = "Really you should accept this"

    Post(s"/researcher/editTopicRequest/1",InboundTopicRequest(uncontroversialTopic.name,updatedDescription)) ~>
      addCredentials(researcherCredentials) ~>
      route ~> check {
      assertResult(Forbidden)(status)

      val topics = StewardDatabase.db.selectTopics(QueryParameters())
      assertResult(1)(topics.size)

      val topic = topics.head
      assertResult(uncontroversialTopic.name)(topic.name)
      assertResult(uncontroversialTopic.description)(topic.description)
      assertResult(researcherUserName)(topic.createdBy)
      assertResult(stewardUserName)(topic.changedBy)
      assertResult(TopicState.approved)(topic.state)

    }
  }

  "StewardService" should "reject an attempt to edit a topic owned by a different user" in {

    StewardDatabase.db.upsertUser(researcherUser)
    StewardDatabase.db.upsertUser(stewardUser)

    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))

    val updatedDescription = "Really you should accept this"

    Post(s"/researcher/editTopicRequest/1",InboundTopicRequest(uncontroversialTopic.name,updatedDescription)) ~>
      addCredentials(stewardCredentials) ~>
      route ~> check {
      assertResult(Forbidden)(status)

      val topics = StewardDatabase.db.selectTopics(QueryParameters())
      assertResult(1)(topics.size)

      val topic = topics.head
      assertResult(uncontroversialTopic.name)(topic.name)
      assertResult(uncontroversialTopic.description)(topic.description)
      assertResult(researcherUserName)(topic.createdBy)
      assertResult(TopicState.pending)(topic.state)
    }
  }

  "StewardService" should "return the full history of queries as Json" in {

    StewardDatabase.db.upsertUser(researcherUser)
    StewardDatabase.db.upsertUser(stewardUser)

    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))
    StewardDatabase.db.changeTopicState(1,TopicState.approved,stewardUserName)
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(0,"test query","Can we get it back?"))

    Get(s"/steward/queryHistory") ~>
      addCredentials(stewardCredentials) ~>
      route ~> check {

        assertResult(OK)(status)

        val queriesJson = new String(body.data.toByteArray)
        val queries = parse(queriesJson).extract[QueryHistory]

        assertResult(List.empty)(QueryHistory(1,0,List(OutboundShrineQuery(1,0,"test query",researcherOutboundUser,Some(
          OutboundTopic(1,uncontroversialTopic.name,uncontroversialTopic.description,researcherOutboundUser,0,TopicState.approved.name,stewardOutboundUser,0)),"Can we get it back?",TopicState.approved.name,0))).differencesExceptTimes(queries))
      }
    }

  "StewardService" should "return the history of queries for a specific user as Json" in {

    StewardDatabase.db.upsertUser(researcherUser)
    StewardDatabase.db.upsertUser(stewardUser)

    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))
    StewardDatabase.db.changeTopicState(1,TopicState.approved,stewardUserName)
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(0,"test query","Can we get it back?"))

    Get(s"/steward/queryHistory/user/${researcherUserName}") ~>
      addCredentials(stewardCredentials) ~>
      route ~> check {
        assertResult(OK)(status)

        val queriesJson = new String(body.data.toByteArray)
        val queries = parse(queriesJson).extract[QueryHistory]

        assertResult(List.empty)(QueryHistory(1,0,List(OutboundShrineQuery(1,0,"test query",researcherOutboundUser,Some(OutboundTopic(1,uncontroversialTopic.name,uncontroversialTopic.description,researcherOutboundUser,0,TopicState.approved.name,stewardOutboundUser,0)),"Can we get it back?",TopicState.approved.name,0))).differencesExceptTimes(queries))
      }
    }

  "StewardService" should "return the query history as Json, filtered by topic" in {

    StewardDatabase.db.upsertUser(researcherUser)
    StewardDatabase.db.upsertUser(stewardUser)
    createSixQueries()

    Get(s"/steward/queryHistory") ~>
      addCredentials(stewardCredentials) ~> route ~> check {
      assertResult(OK)(status)

      val queriesJson = new String(body.data.toByteArray)
      val queries = parse(queriesJson).extract[QueryHistory]

      assertResult(6)(queries.queryRecords.size)
    }

    Get(s"/steward/queryHistory/user/ben") ~>
      addCredentials(stewardCredentials) ~> route ~> check {
      assertResult(OK)(status)

      val queriesJson = new String(body.data.toByteArray)
      val queries = parse(queriesJson).extract[QueryHistory]

      assertResult(6)(queries.queryRecords.size)
    }

    Get(s"/steward/queryHistory/topic/1") ~>
      addCredentials(stewardCredentials) ~> route ~> check {
      assertResult(OK)(status)

      val queriesJson = new String(body.data.toByteArray)
      val queries = parse(queriesJson).extract[QueryHistory]

      assertResult(3)(queries.queryRecords.size)
    }

    Get(s"/steward/queryHistory/user/ben/topic/1") ~>
      addCredentials(stewardCredentials) ~> route ~> check {
      assertResult(OK)(status)

      val queriesJson = new String(body.data.toByteArray)
      val queries = parse(queriesJson).extract[QueryHistory]

      assertResult(3)(queries.queryRecords.size)
    }

    Get(s"/steward/queryHistory/topic/1?skip=1&limit=2") ~>
      addCredentials(stewardCredentials) ~> route ~> check {
      assertResult(OK)(status)

      val queriesJson = new String(body.data.toByteArray)
      val queries = parse(queriesJson).extract[QueryHistory]

      assertResult(2)(queries.queryRecords.size)
    }

    Get(s"/steward/queryHistory/topic/2") ~>
      addCredentials(stewardCredentials) ~> route ~> check {
      assertResult(OK)(status)

      val queriesJson = new String(body.data.toByteArray)
      val queries = parse(queriesJson).extract[QueryHistory]

      assertResult(3)(queries.queryRecords.size)
    }
  }

  "StewardService" should "return counts of queries, total and by user" in {

    StewardDatabase.db.upsertUser(researcherUser)
    StewardDatabase.db.upsertUser(stewardUser)
    createSixQueries()

    Get(s"/steward/statistics/queriesPerUser") ~>
      addCredentials(stewardCredentials) ~> route ~> check {
      assertResult(OK)(status)

      val statisticsJson = new String(body.data.toByteArray)
      val statistics = parse(statisticsJson).extract[QueriesPerUser]

      assertResult(6)(statistics.total)

      assertResult(QueriesPerUser(6,Seq((researcherOutboundUser,6))))(statistics)
    }
  }

  "StewardService" should "return the topics for a specific user as Json" in {

    StewardDatabase.db.upsertUser(researcherUser)
    StewardDatabase.db.upsertUser(stewardUser)
    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))
    Get(s"/steward/topics/user/${researcherUserName}") ~>
      addCredentials(stewardCredentials) ~>
      route ~> check {

        assertResult(OK)(status)
        val topicsJson = new String(body.data.toByteArray)
        val topics:StewardsTopics = parse(topicsJson).extract[StewardsTopics]

        assertResult(true)(StewardsTopics(1,0,Seq(uncontroversialTopic)).sameExceptForTimes(topics))
    }
  }

  "StewardService" should "return the topics for a specific user as Json, given skip and limit parameters" in {

    StewardDatabase.db.upsertUser(researcherUser)
    StewardDatabase.db.upsertUser(stewardUser)
    createFiveTopics()

    Get(s"/steward/topics/user/${researcherUserName}") ~>
      addCredentials(stewardCredentials) ~>
      route ~> check {

      assertResult(OK)(status)
      val topicsJson = new String(body.data.toByteArray)
      val topics:StewardsTopics = parse(topicsJson).extract[StewardsTopics]

      assertResult(5)(topics.topics.size)
    }

    Get(s"/steward/topics/user/${researcherUserName}?skip=0&limit=3") ~>
      addCredentials(stewardCredentials) ~>
      route ~> check {

      assertResult(OK)(status)
      val topicsJson = new String(body.data.toByteArray)
      val topics:StewardsTopics = parse(topicsJson).extract[StewardsTopics]

      assertResult(3)(topics.topics.size)
    }

    Get(s"/steward/topics/user/${researcherUserName}?skip=2&limit=3") ~>
      addCredentials(stewardCredentials) ~>
      route ~> check {

      assertResult(OK)(status)
      val topicsJson = new String(body.data.toByteArray)
      val topics:StewardsTopics = parse(topicsJson).extract[StewardsTopics]

      assertResult(3)(topics.topics.size)
    }

    Get(s"/steward/topics/user/${researcherUserName}?skip=3&limit=4") ~>
      addCredentials(stewardCredentials) ~>
      route ~> check {

      assertResult(OK)(status)
      val topicsJson = new String(body.data.toByteArray)
      val topics:StewardsTopics = parse(topicsJson).extract[StewardsTopics]

      assertResult(2)(topics.topics.size)
    }
  }

  "StewardService" should "return all of the topics" in {
    StewardDatabase.db.upsertUser(researcherUser)
    StewardDatabase.db.upsertUser(stewardUser)
    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))
    Get(s"/steward/topics") ~>
      addCredentials(stewardCredentials) ~>
      route ~> check {
      assertResult(OK)(status)
      val topicsJson = new String(body.data.toByteArray)
      val topics = parse(topicsJson).extract[StewardsTopics]

      assertResult(true)(StewardsTopics(1,0,Seq(uncontroversialTopic)).sameExceptForTimes(topics))
    }
  }

  "StewardService" should "return all of the pending topics" in {
    StewardDatabase.db.upsertUser(researcherUser)
    StewardDatabase.db.upsertUser(stewardUser)
    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))

    Get(s"/steward/topics?state=Pending") ~>
      addCredentials(stewardCredentials) ~>
      route ~> check {
      assertResult(OK)(status)
      val topicsJson = new String(body.data.toByteArray)
      val topics = parse(topicsJson).extract[StewardsTopics]

      assertResult(true)(StewardsTopics(1,0,Seq(uncontroversialTopic)).sameExceptForTimes(topics))
    }
  }

  "StewardService" should "approve a researcher's request to study a topic" in {
    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))

    Post(s"/steward/approveTopic/topic/${uncontroversialTopic.id}") ~>
      addCredentials(stewardCredentials) ~>
      route ~> check {
        assertResult(OK)(status)
    }
  }

  "StewardService" should "reject a researcher's request to study a topic" in {
    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))

    Post(s"/steward/rejectTopic/topic/${uncontroversialTopic.id}") ~>
      addCredentials(stewardCredentials) ~>
      route ~> check {
        assertResult(OK)(status)
    }
  }

  "A steward's attempt to approve or reject a topic that doesn't exist" should "report an error" in {
    Post(s"/steward/approveTopic/topic/${uncontroversialTopic.id}") ~>
      addCredentials(stewardCredentials) ~>
      route ~> check {
        assertResult(UnprocessableEntity)(status)
    }

    Post(s"/steward/rejectTopic/topic/${uncontroversialTopic.id}") ~>
      addCredentials(stewardCredentials) ~>
      route ~> check {
      assertResult(UnprocessableEntity)(status)
    }
  }

  "StewardService" should  "redirect several urls to client/index.html" in {

    Get() ~>
      route ~> check {
      status === PermanentRedirect
      header("Location") === "client/index.html"
    }


    Get("/") ~>
      route ~> check {
      status === PermanentRedirect
      header("Location") === "client/index.html"
    }

    Get("/index.html") ~>
      route ~> check {

      status === PermanentRedirect
      header("Location") === "client/index.html"
    }

  }

  /*
  //CORS won't be turned on in the real server, so this test won't normally pass.

    "DataStewardService" should "support a CORS OPTIONS request" in {
      Options(s"/steward/topics") ~>
        //No credentials for Options, so no addCredentials(testCredentials) ~>
        route ~> check {
          assertResult(OK)(status)
      }
    }
  */
/* For some reason, the test engine isn't finding the static resources. I suspect it is testing with raw .class files ,
not an actual .war or .jar. Works on the actual server.
  "StewardService" should "serve up static files from resources/client" in {
    Get("/steward/client/test.txt") ~> route ~> check {
      assertResult(OK)(status)
      assertResult("Test file")(body.asString)
    }
  }
*/

  def createFiveTopics(): Unit ={
    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))
    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest("slightlyControversial","who cares?"))
    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest("controversial","controversial"))
    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest("moderatelyControversial","more controversial than that"))
    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest("veryControversial","Just say no"))
  }

  def createSixQueries() = {
    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest(uncontroversialTopic.name,uncontroversialTopic.description))
    StewardDatabase.db.changeTopicState(1,TopicState.approved,stewardUserName)

    StewardDatabase.db.createRequestForTopicAccess(researcherUser,InboundTopicRequest("Forbidden topic","No way is the data steward going for this"))
    StewardDatabase.db.changeTopicState(2,TopicState.rejected,stewardUserName)

    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(0,"A test query","Can we get it back?"))
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(2),InboundShrineQuery(1,"B forbidden query","Can we get it back?"))
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(2," C test query","Can we get it back?"))
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(2),InboundShrineQuery(3,"4 forbidden query","Can we get it back?"))
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(1),InboundShrineQuery(4,"7 test query","Can we get it back?"))
    StewardDatabase.db.logAndCheckQuery(researcherUserName,Some(2),InboundShrineQuery(5,"% forbidden query","Can we get it back?"))
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

/*
object OutboundTopic {

  val uncontroversialTopic = OutboundTopic(1,"UncontroversialKidneys","Study kidneys without controversy",OutboundUser.someOutboundResearcher,0L,TopicState.pending.name,OutboundUser.someOutboundResearcher,0L)
  val forbiddenTopicId = 0

}

object OutboundShrineQuery extends ((
                                      StewardQueryId,
                                      ExternalQueryId,
                                      String,
                                      OutboundUser,
                                      Option[OutboundTopic],
                                      QueryContents,
                                      TopicStateName,
                                      Date) => OutboundShrineQuery) {
  val notSureAboutQueryContents:QueryContents = "Appropriate query contents"

  val someQueryRecord = OutboundShrineQuery(-5,-2,"Kidney Query",OutboundUser.someOutboundResearcher,Some(uncontroversialTopic),OutboundShrineQuery.notSureAboutQueryContents,TopicState.approved.name,System.currentTimeMillis())
}
 */