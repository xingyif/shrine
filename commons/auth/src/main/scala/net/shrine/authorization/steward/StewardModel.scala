package net.shrine.authorization.steward

import java.lang.reflect.Field

import net.shrine.i2b2.protocol.pm.User
import net.shrine.log.Loggable
import net.shrine.protocol.Credential
import net.shrine.serialization.NodeSeqSerializer
import org.json4s.{DefaultFormats, DefaultJsonFormats, Formats}
import spray.http.{StatusCode, StatusCodes}
import spray.httpx.Json4sSupport

import scala.util.{Failure, Try}
import scala.xml.NodeSeq

/**
 * Data model for the data steward.
 *
 * @author dwalend
 * @since 1.19
 */


//http response json
case class OutboundTopic(id:TopicId,
                         name:String,
                         description:String,
                         createdBy:OutboundUser,
                         createDate:Date,
                         state:TopicStateName,
                         changedBy:OutboundUser,
                         changeDate:Date
                          ) {

  def differences(other:OutboundTopic):Seq[(String,Any,Any)] = {
    if (this == other) List()
    else {
      val fields = getClass.getDeclaredFields
      val names = fields.map(_.getName)

      def getFromField(field:Field,thing:OutboundTopic):Any = {
        field.setAccessible(true)
        field.get(thing)
      }

      val thisUnapplied = fields.map(getFromField(_,this))
      val otherUnapplied = fields.map(getFromField(_,other))

      val tuples = names.zip(thisUnapplied.zip(otherUnapplied))

      def difference(name:String,one:Any,other:Any):Option[(String,Any,Any)] = {
        if(one == other) None
        else {
          Some((name,one,other))
        }
      }
      tuples.map(x => difference(x._1,x._2._1,x._2._2)).to[Seq].flatten
    }
  }
  def differencesExceptTimes(other:OutboundTopic):Seq[(String,Any,Any)] = {
    differences(other).filterNot(x => x._1 == "createDate").filterNot(x => x._1 == "changeDate")
  }
}

case class OutboundUser(userName:UserName,fullName:String,roles:Set[Role] = Set(researcherRole)) extends DefaultJsonFormats

object OutboundUser extends Loggable with Json4sSupport {
  override implicit def json4sFormats: Formats = DefaultFormats

  def createResearcher(userName:UserName,fullName:String) = OutboundUser(userName,fullName)
  def createSteward(userName:UserName,fullName:String) = OutboundUser(userName,fullName,Set(researcherRole,stewardRole))
  def createFromUser(user:User) = if( user.params.toList.contains((stewardRole,"true"))) createSteward(user.username,user.fullName)
                                  else createResearcher(user.username,user.fullName)
  /** If the user is unknown but has a user id, best effort is to guess this user is a researcher with userName as their full name*/
  def createUnknownUser(userName:UserName) = {
    info(s"Creating an OutboundUser for unknown userName $userName")
    createResearcher(userName,userName)
  }

}

sealed case class TopicState(name:TopicStateName,statusCode:StatusCode,message:String)

//todo split out TopicResponse from TopicState
object TopicState {
  
  val pending = TopicState("Pending",StatusCodes.UnavailableForLegalReasons,"Topic pending data steward's approval")
  val approved = TopicState("Approved",StatusCodes.OK,"OK")
  val rejected = TopicState("Rejected",StatusCodes.UnavailableForLegalReasons,"Topic rejected by data steward")

  val unknownForUser = TopicState("Unknown For User",StatusCodes.UnprocessableEntity,"Topic unknown for user")
  val createTopicsModeRequiresTopic = TopicState("Required But Not Supplied",StatusCodes.UnprocessableEntity,"Topic required but not supplied")

  val namesToStates = Seq(pending,approved,rejected,unknownForUser,createTopicsModeRequiresTopic).map(x => x.name -> x).toMap

  def stateForStringOption(stringOption: Option[TopicStateName]): Try[Option[TopicState]] = {

    val notPresent: Try[Option[TopicState]] = Try(None)
    stringOption.fold(notPresent)((string: TopicStateName) => {

      val notInTheMap: Try[Option[TopicState]] = Failure(new IllegalArgumentException(s"No value for $string in $namesToStates"))
      namesToStates.get(string).fold(notInTheMap)(x => Try(Some(x)))
    })
  }
}

case class OutboundShrineQueryWithJson(
                                stewardId:StewardQueryId,
                                externalId:ExternalQueryId,
                                name:String,
                                user:OutboundUser,
                                topic:Option[OutboundTopic],
                                queryContents: NodeSeq,
                                stewardResponse:TopicStateName,
                                date:Date)
  extends DefaultJsonFormats
{
  def convertToXml:OutboundShrineQuery = {
    OutboundShrineQuery(stewardId, externalId, name, user, topic, queryContents.toString, stewardResponse, date)
  }
}

case class OutboundShrineQuery(
                                stewardId:StewardQueryId,
                                externalId:ExternalQueryId,
                                name:String,
                                user:OutboundUser,
                                topic:Option[OutboundTopic],
                                queryContents: QueryContents,
                                stewardResponse:TopicStateName,
                                date:Date) {
  def differences(other:OutboundShrineQuery):Seq[(String,Any,Any)] = {
    if (this == other) List()
    else {
      val fields = getClass.getDeclaredFields
      val names = fields.map(_.getName)

      def getFromField(field: Field, thing: OutboundShrineQuery): Any = {
        field.setAccessible(true)
        field.get(thing)
      }

      val thisUnapplied = fields.map(getFromField(_, this))
      val otherUnapplied = fields.map(getFromField(_, other))

      val tuples = names.zip(thisUnapplied.zip(otherUnapplied))

      def difference(name: String, one: Any, other: Any): Option[(String, Any, Any)] = {
        // TODO: Remove this horrible string equality hack.
        if (one == other)
          None
        else {
          Some((name, one, other))
        }
      }
      tuples.map(x => difference(x._1, x._2._1, x._2._2)).to[Seq].flatten
    }
  }

  def convertToJson:OutboundShrineQueryWithJson = {
    OutboundShrineQueryWithJson(stewardId, externalId, name, user, topic, scala.xml.XML.loadString(queryContents), stewardResponse, date)
  }

  def differencesExceptTimes(other:OutboundShrineQuery):Seq[(String,Any,Any)] = {
    val diffWithoutTimes = differences(other).filterNot(x => x._1 == "date").filterNot(x => x._1 == "topic")
    val topicDiffs:Seq[(String,Any,Any)] = (topic,other.topic) match {
      case (None,None) => Seq.empty[(String,Any,Any)]
      case (Some(thisTopic),Some(otherTopic)) => thisTopic.differencesExceptTimes(otherTopic)
      case _ => Seq(("topic",this.topic,other.topic))
    }
    diffWithoutTimes ++ topicDiffs
  }
}

case class QueriesPerUser(total:Int,queriesPerUser:Seq[(OutboundUser,Int)]) //todo rename QueriesPerResearcher

case class TopicsPerState(total:Int,topicsPerState:Seq[(TopicStateName,Int)])

case class ResearchersTopics(userId:UserName,totalCount:Int,skipped:Int,topics:Seq[OutboundTopic]) {
  def sameExceptForTimes(researchersTopics: ResearchersTopics):Boolean = {
    (totalCount == researchersTopics.totalCount) &&
      (skipped == researchersTopics.skipped) &&
      (userId == researchersTopics.userId) &&
      (topics.size == researchersTopics.topics.size) &&
      topics.zip(researchersTopics.topics).forall(x => x._1.id == x._2.id)
  }
}

case class StewardsTopics(totalCount:Int,skipped:Int,topics:Seq[OutboundTopic]) {
  def sameExceptForTimes(stewardsTopics: StewardsTopics):Boolean = {
    (totalCount == stewardsTopics.totalCount) &&
      (skipped == stewardsTopics.skipped) &&
      (topics.size == stewardsTopics.topics.size) &&
      topics.zip(stewardsTopics.topics).forall(x => x._1.id == x._2.id)
  }
}

case class QueryHistory(totalCount:Int,skipped:Int,queryRecords:Seq[OutboundShrineQuery]) {

  def sameExceptForTimes(queryResponse: QueryHistory):Boolean = {
    (totalCount == queryResponse.totalCount) &&
      (skipped == queryResponse.skipped) &&
      (queryRecords.size == queryResponse.queryRecords.size) &&
      queryRecords.zip(queryResponse.queryRecords).forall(x => x._1.differencesExceptTimes(x._2).isEmpty)
  }

  def differences(other:QueryHistory):Seq[(String,Any,Any)] = {
    if (this == other) List()
    else {
      val fields = getClass.getDeclaredFields
      val names = fields.map(_.getName)

      def getFromField(field:Field,thing:QueryHistory):Any = {
        field.setAccessible(true)
        field.get(thing)
      }

      val thisUnapplied = fields.map(getFromField(_,this))
      val otherUnapplied = fields.map(getFromField(_,other))

      val tuples = names.zip(thisUnapplied.zip(otherUnapplied))

      def difference(name:String,one:Any,other:Any):Option[(String,Any,Any)] = {
        if(one == other) None
        else {
          Some((name,one,other))
        }
      }
      tuples.map(x => difference(x._1,x._2._1,x._2._2)).to[Seq].flatten
    }
  }

  def differencesExceptTimes(other:QueryHistory):Seq[(String,Any,Any)] = {
    val normalDiffs:Seq[(String,Any,Any)] = differences(other).filterNot(x => x._1 == "queryRecords")
    val timeDiffs:Seq[(String,Any,Any)] = queryRecords.zip(other.queryRecords).flatMap(x => x._1.differencesExceptTimes(x._2))
    normalDiffs ++ timeDiffs
  }

  def convertToJson = QueryHistoryWithJson(totalCount, skipped, queryRecords.map(_.convertToJson))
}

case class QueryHistoryWithJson(totalCount:Int,skipped:Int,queryRecords:Seq[OutboundShrineQueryWithJson]) extends Json4sSupport {
  implicit def json4sFormats: Formats = DefaultFormats + new NodeSeqSerializer
  def convertToXml = QueryHistory(totalCount, skipped, queryRecords.map(_.convertToXml))
}

case class TopicIdAndName(id:String,name:String)

case class ResearcherToAudit(researcher:OutboundUser, count:Int, leastRecentQueryDate:Date, currentAuditDate:Date) {
  def sameExceptForTimes(audit: ResearcherToAudit): Boolean = {
    (researcher == audit.researcher) &&
      (count == audit.count)
  }
}


//http request Json
case class InboundShrineQuery(
                              externalId:ExternalQueryId,
                              name:String,
                              queryContents: QueryContents)

case class InboundTopicRequest(name:String,description:String)