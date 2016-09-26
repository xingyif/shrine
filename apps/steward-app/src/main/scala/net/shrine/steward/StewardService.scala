package net.shrine.steward

import akka.actor.Actor
import akka.event.Logging
import net.shrine.authentication.UserAuthenticator
import net.shrine.authorization.steward._
import net.shrine.i2b2.protocol.pm.User
import net.shrine.serialization.NodeSeqSerializer
import net.shrine.steward.db._
import net.shrine.steward.pmauth.Authorizer
import org.json4s.native.Serialization
import shapeless.HNil
import spray.http.{HttpRequest, HttpResponse, StatusCodes}
import spray.httpx.Json4sSupport
import spray.routing.directives.LogEntry
import spray.routing._
import org.json4s.{DefaultFormats, DefaultJsonFormats, Formats, Serialization}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor

class StewardServiceActor extends Actor with StewardService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(route)
}


// this trait defines our service behavior independently from the service actor
trait StewardService extends HttpService with Json4sSupport {
  implicit def json4sFormats: Formats = DefaultFormats + new NodeSeqSerializer

  val userAuthenticator = UserAuthenticator(StewardConfigSource.config)

  //don't need to do anything special for unauthorized users, but they do need access to a static form.
  lazy val route:Route = gruntWatchCorsSupport{
      requestLogRoute ~ fullLogRoute
  }

  lazy val requestLogRoute = logRequestResponse(logEntryForRequest _) {
    redirectToIndex ~ staticResources ~ makeTrouble ~ about
  }

  lazy val fullLogRoute = logRequestResponse(logEntryForRequestResponse _) {
    qepRoute ~ authenticatedInBrowser
  }

  // logs just the request method, uri and response at info level
  //logging is controlled by Akka's config, slf4j, and log4j config
   def logEntryForRequestResponse(req: HttpRequest): Any => Option[LogEntry] = {
    case res: HttpResponse => {
      Some(LogEntry(s"\n  Request: $req \n  Response: $res", Logging.InfoLevel))
    }
    case _ => None // other kind of responses
  }

  // logs just the request method, uri and response status at info level
  def logEntryForRequest(req: HttpRequest): Any => Option[LogEntry] = {
    case res: HttpResponse => {
      Some(LogEntry(s"\n  Request: $req \n  Response status: ${res.status}", Logging.InfoLevel))
    }
    case _ => None // other kind of responses
  }

  //pathPrefixTest shields the QEP code from the redirect.
  def authenticatedInBrowser: Route = pathPrefixTest("user"|"steward"|"researcher") {
    reportIfFailedToAuthenticate {
        authenticate(userAuthenticator.basicUserAuthenticator) { user =>

          StewardDatabase.db.upsertUser(user)

          pathPrefix("user") {userRoute(user)} ~
          pathPrefix("steward") {stewardRoute(user)} ~
          pathPrefix("researcher") {researcherRoute(user)}
      }
    }
  }

  val reportIfFailedToAuthenticate = routeRouteResponse {
    case Rejected(List(AuthenticationFailedRejection(_,_))) =>
      complete("AuthenticationFailed")
  }

  def makeTrouble = pathPrefix("makeTrouble") {
    complete(throw new IllegalStateException("fake trouble"))
  }

  lazy val redirectToIndex = pathEnd {
    redirect("steward/client/index.html", StatusCodes.PermanentRedirect) //todo pick up "steward" programatically
  } ~
  ( path("index.html") | pathSingleSlash) {
      redirect("client/index.html", StatusCodes.PermanentRedirect)
  }

  lazy val staticResources = pathPrefix("client") {
    pathEnd {
      redirect("client/index.html", StatusCodes.PermanentRedirect)
    } ~
    pathSingleSlash {
      redirect("index.html", StatusCodes.PermanentRedirect)
    } ~ {
      getFromResourceDirectory("client")
    }
  }

  lazy val about = pathPrefix("about") {
    path("createTopicsMode") {
      get {
        complete(StewardConfigSource.createTopicsInState.name)
      }
    }
  }

  def userRoute(user:User):Route = get {
    pathPrefix("whoami") {
      complete(OutboundUser.createFromUser(user))
    }
  }

  def qepRoute:Route = pathPrefix("qep") {
    authenticate(userAuthenticator.basicUserAuthenticator) { user =>

      StewardDatabase.db.upsertUser(user)

      authorize(Authorizer.authorizeQep(user)) {
        pathPrefix("requestQueryAccess") ( requestQueryAccess ) ~
        pathPrefix("approvedTopics") ( getApprovedTopicsForUser )
      }
    }
  }

  def requestQueryAccess:Route = post {
    requestQueryAccessWithTopic ~ requestQueryAccessWithoutTopic
  }

  def requestQueryAccessWithTopic:Route = path("user" /Segment/ "topic" / IntNumber) { (userId,topicId) =>
    entity(as[InboundShrineQuery]) { shrineQuery:InboundShrineQuery =>
      //todo really pull the user out of the shrine query and check vs the PM. If they aren't there, reject them for this new reason
      val result: (TopicState, Option[TopicIdAndName]) = StewardDatabase.db.logAndCheckQuery(userId,Some(topicId),shrineQuery)

      respondWithStatus(result._1.statusCode) {
        if(result._1.statusCode == StatusCodes.OK) complete (result._2.getOrElse(""))
        else complete(result._1.message)
      }
    }
  }

  def requestQueryAccessWithoutTopic:Route = path("user" /Segment) { userId =>
    entity(as[InboundShrineQuery]) { shrineQuery:InboundShrineQuery =>
      //todo really pull the user out of the shrine query and check vs the PM. If they aren't there, reject them for this new reason
      val result = StewardDatabase.db.logAndCheckQuery(userId,None,shrineQuery)
      respondWithStatus(result._1.statusCode) {
        if(result._1.statusCode == StatusCodes.OK) complete (result._2)
        else complete(result._1.message)
      }
    }
  }

  lazy val getApprovedTopicsForUser:Route = get {
    //todo change to "researcher"
    path("user" /Segment) { userId =>
      //todo really pull the user out of the shrine query and check vs the PM. If they aren't there, reject them for this new reason
      val queryParameters = QueryParameters(researcherIdOption = Some(userId),stateOption = Some(TopicState.approved))
      val researchersTopics = StewardDatabase.db.selectTopicsForResearcher(queryParameters)

      complete(researchersTopics)
    }
  }

  def researcherRoute(user:User):Route = authorize(Authorizer.authorizeResearcher(user)) {
    pathPrefix("topics") { getUserTopics(user.username) } ~
      pathPrefix("queryHistory") { getUserQueryHistory(Some(user.username)) } ~
      pathPrefix("requestTopicAccess") { requestTopicAccess(user) } ~
      pathPrefix("editTopicRequest") { editTopicRequest(user) }
  }

  def getUserTopics(userId:UserName):Route = get {
    //lookup topics for this user in the db
   matchQueryParameters(Some(userId)){queryParameters:QueryParameters =>
      val researchersTopics = StewardDatabase.db.selectTopicsForResearcher(queryParameters)
      complete(researchersTopics)
    }
  }

  def matchQueryParameters(userName: Option[UserName])(parameterRoute:QueryParameters => Route): Route =  {

    parameters('state.?,'skip.as[Int].?,'limit.as[Int].?,'sortBy.as[String].?,'sortDirection.as[String].?,'minDate.as[Date].?,'maxDate.as[Date].?) { (stateStringOption,skipOption,limitOption,sortByOption,sortOption,minDate,maxDate) =>

      val stateTry = TopicState.stateForStringOption(stateStringOption)
      stateTry match {
        case Success(stateOption) =>
          val qp = QueryParameters(userName,
            stateOption,
            skipOption,
            limitOption,
            sortByOption,
            SortOrder.sortOrderForStringOption(sortOption),
            minDate,
            maxDate
          )

          parameterRoute(qp)

        case Failure(ex) => badStateRoute(stateStringOption)
      }
    }
  }

  def badStateRoute(stateStringOption:Option[String]):Route = {
    respondWithStatus(StatusCodes.UnprocessableEntity) {
      complete(s"Topic state ${stateStringOption.getOrElse(s"$stateStringOption (stateStringOption should never be None at this point)")} unknown. Please specify one of ${TopicState.namesToStates.keySet}")
    }
  }

  def getUserQueryHistory(userIdOption:Option[UserName]):Route = get {
    parameter('asJson.as[Boolean].?) { asJson =>
      path("topic" / IntNumber) { topicId: TopicId =>
        getQueryHistoryForUserByTopic(userIdOption, Some(topicId), asJson)
      } ~
        getQueryHistoryForUserByTopic(userIdOption, None, asJson)
    }
  }

  def getQueryHistoryForUserByTopic(userIdOption:Option[UserName],topicIdOption:Option[TopicId], asJson: Option[Boolean]) = get {
    matchQueryParameters(userIdOption) { queryParameters:QueryParameters =>
      val queryHistory = StewardDatabase.db.selectQueryHistory(queryParameters, topicIdOption)

      if (asJson.getOrElse(false)) complete(queryHistory.convertToJson) else complete(queryHistory)
    }
  }

  def requestTopicAccess(user:User):Route = post {
    entity(as[InboundTopicRequest]) { topicRequest: InboundTopicRequest =>
      //todo notify the data stewards
      StewardDatabase.db.createRequestForTopicAccess(user,topicRequest)

      complete(StatusCodes.Accepted)
    }
  }

  def editTopicRequest(user:User):Route = post {
    path(IntNumber) { topicId => 
      entity(as[InboundTopicRequest]) { topicRequest: InboundTopicRequest =>
        //todo notify the data stewards
        val updatedTopicTry:Try[OutboundTopic] = StewardDatabase.db.updateRequestForTopicAccess(user, topicId, topicRequest)

        updatedTopicTry match {
          case Success(updatedTopic) =>
            respondWithStatus(StatusCodes.Accepted) {
              complete(updatedTopic)
            }

          case Failure(x) => x match {
            case x:TopicDoesNotExist => respondWithStatus(StatusCodes.NotFound) {
              complete(x.getMessage)
            }
            case x:ApprovedTopicCanNotBeChanged => respondWithStatus(StatusCodes.Forbidden) {
              complete(x.getMessage)
            }
            case x:DetectedAttemptByWrongUserToChangeTopic => respondWithStatus(StatusCodes.Forbidden) {
              complete(x.getMessage)
            }
            case _ => throw x
          }
        }
      }
    }
  }

  def stewardRoute(user:User):Route = authorize(Authorizer.authorizeSteward(user)) {
    pathPrefix("queryHistory" / "user") {getUserQueryHistory } ~
      pathPrefix("queryHistory") {getQueryHistory} ~
      pathPrefix("topics" / "user")(getUserTopicsForSteward) ~
      path("topics"){getTopicsForSteward} ~
      pathPrefix("approveTopic")(approveTopicForUser(user)) ~
      pathPrefix("rejectTopic")(rejectTopicForUser(user)) ~
      pathPrefix("statistics"){getStatistics}
  }

  lazy val getUserQueryHistory:Route = pathPrefix(Segment) { userId =>
    getUserQueryHistory(Some(userId))
  }

  lazy val getQueryHistory:Route = getUserQueryHistory(None)

  lazy val getTopicsForSteward:Route = getTopicsForSteward(None)

  lazy val getUserTopicsForSteward:Route = path(Segment) { userId =>
    getTopicsForSteward(Some(userId))
  }

  def getTopicsForSteward(userIdOption:Option[UserName]):Route = get {
    //lookup topics for this user in the db
    matchQueryParameters(userIdOption) { queryParameters: QueryParameters =>
      val stewardsTopics:StewardsTopics = StewardDatabase.db.selectTopicsForSteward(queryParameters)

      complete(stewardsTopics)
    }
  }

  def approveTopicForUser(user:User):Route = changeStateForTopic(TopicState.approved,user)

  def rejectTopicForUser(user:User):Route = changeStateForTopic(TopicState.rejected,user)

  def changeStateForTopic(state:TopicState,user:User):Route = post {
    path("topic" / IntNumber) { topicId =>
      StewardDatabase.db.changeTopicState(topicId, state, user.username).fold(respondWithStatus(StatusCodes.UnprocessableEntity){
        complete(s"No topic found for $topicId")
      })(topic => complete(StatusCodes.OK))
    }
  }

  def getStatistics:Route = pathPrefix("queriesPerUser"){getQueriesPerUser} ~
                              pathPrefix("topicsPerState"){getTopicsPerState}

  def getQueriesPerUser:Route = get{
    matchQueryParameters(None) { queryParameters: QueryParameters =>
      val result = StewardDatabase.db.selectShrineQueryCountsPerUser(queryParameters)

      complete(result)
    }
  }

  def getTopicsPerState:Route = get{
    matchQueryParameters(None) { queryParameters: QueryParameters =>
      val result = StewardDatabase.db.selectTopicCountsPerState(queryParameters)
      complete(result)
    }
  }
}

//adapted from https://gist.github.com/joseraya/176821d856b43b1cfe19
object gruntWatchCorsSupport extends Directive0 with RouteConcatenation {

  import spray.http.HttpHeaders.{`Access-Control-Allow-Methods`, `Access-Control-Max-Age`, `Access-Control-Allow-Headers`,`Access-Control-Allow-Origin`}
  import spray.routing.directives.RespondWithDirectives.respondWithHeaders
  import spray.routing.directives.MethodDirectives.options
  import spray.routing.directives.RouteDirectives.complete
  import spray.http.HttpMethods.{OPTIONS,GET,POST}
  import spray.http.AllOrigins

  private val allowOriginHeader = `Access-Control-Allow-Origin`(AllOrigins)
  private val optionsCorsHeaders = List(
    `Access-Control-Allow-Headers`("Origin, X-Requested-With, Content-Type, Accept, Accept-Encoding, Accept-Language, Host, Referer, User-Agent, Authorization"),
    `Access-Control-Max-Age`(1728000)) //20 days

  val gruntWatch:Boolean = StewardConfigSource.config.getBoolean("shrine.steward.gruntWatch")

  override def happly(f: (HNil) => Route): Route = {
    if(gruntWatch) {
      options {
        respondWithHeaders(`Access-Control-Allow-Methods`(OPTIONS, GET, POST) ::  allowOriginHeader :: optionsCorsHeaders){
          complete(StatusCodes.OK)
        }
      } ~ f(HNil)
    }
    else f(HNil)
  }
}
