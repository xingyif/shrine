package net.shrine.steward

import javax.mail.internet.InternetAddress

import akka.actor.{Actor, ActorSystem, Props}
import courier.{Envelope, Text}
import net.shrine.authorization.steward.ResearcherToAudit
import net.shrine.email.ConfiguredMailer
import net.shrine.log.Loggable
import net.shrine.steward.db.StewardDatabase
import spray.servlet.WebBoot

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import courier._
import java.util.Date

import net.shrine.config.{ConfigExtensions, DurationConfigParser}

import scala.language.postfixOps

// this class is instantiated by the servlet initializer
// it needs to have a default constructor and implement
// the spray.servlet.WebBoot trait
class Boot extends WebBoot with Loggable {

  info(s"StewardActors akka daemonic config is ${StewardConfigSource.config.getString("akka.daemonic")}")

  val warmUp:Unit = StewardDatabase.warmUp()

  // we need an ActorSystem to host our application in
  val system = ActorSystem("StewardActors",StewardConfigSource.config)

  // the service actor replies to incoming HttpRequests
  val serviceActor = system.actorOf(Props[StewardServiceActor])

  // if sending email alerts is on start a periodic polling of the database at a fixed time every day.
  // if either the volume or time conditions are met, send an email to the data steward asking for an audit
  val config = StewardConfigSource.config

  val emailConfig = config.getConfig("shrine.steweard.emailDataSteward")

  if(emailConfig.getBoolean("sendAuditEmails")) {
    system.scheduler.schedule(initialDelay = 0 milliseconds, //todo figure out how to handle the initial delay
      interval = emailConfig.getConfigured("interval",DurationConfigParser.apply),
      receiver = system.actorOf(Props[AuditEmailerActor]),
      "tick")
  }

  //todo use this to figure out what if any initial delay should be. Maybe if the interval is >= 1 day then the delay will send the email so many hours passed either the previous or the next midnight
  def previousMidnight: Long = {
    import java.util.Calendar
    val c = Calendar.getInstance()
    val now = c.getTimeInMillis()
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    c.getTimeInMillis
  }
}

class AuditEmailerActor extends Actor {

  override def receive: Receive = {case _ => AuditEmailer.audit()}

}

object AuditEmailer  {

  //todo check the config and fail early if something important is missing.
  val config = StewardConfigSource.config
  val mailer = ConfiguredMailer.createMailerFromConfig(config.getConfig("shrine.email"))
  val emailConfig = config.getConfig("shrine.steweard.emailDataSteward")

  val maxQueryCountBetweenAudits = emailConfig.getInt("maxQueryCountBetweenAudits")
  val minTimeBetweenAudits = emailConfig.getConfigured("minTimeBetweenAudits",DurationConfigParser.apply)
  val researcherLineTemplate = emailConfig.getString("researcherLine")
  val emailTemplate = emailConfig.getString("emailBody")
  val emailSubject = emailConfig.getString("subject")
  val from = emailConfig.get("from",new InternetAddress(_))
  val to = emailConfig.get("to",new InternetAddress(_))
  val stewardBaseUrl: Option[String] = config.getOption("stewardBaseUrl",_.getString)

  def audit() = {
    //gather a list of users to audit
    val researchersToAudit: Seq[ResearcherToAudit] = StewardDatabase.db.selectResearchersToAudit(maxQueryCountBetweenAudits,
                                                                                                  minTimeBetweenAudits)
    if (researchersToAudit.nonEmpty){

      val auditLines = researchersToAudit.sortBy(_.count).reverse.map { researcher =>
        researcherLineTemplate.replaceAll("FULLANME",researcher.researcher.fullName)
          .replaceAll("USERNAME",researcher.researcher.userName)
          .replaceAll("COUNT",researcher.count.toString)
          .replaceAll("LAST_AUDIT_DATE",new Date(researcher.leastRecentQueryDate).toString)
      }.mkString("\n")

      //build up the email body
      val withLines = emailTemplate.replaceAll("AUDIT_LINES",auditLines)
      val withBaseUrl = stewardBaseUrl.fold(withLines)(withLines.replaceAll("STEWARD_BASE_URL",_))
      val emailBody = Text(withBaseUrl)

      val envelope:Envelope = Envelope.from(from).to(to).subject(emailSubject).content(emailBody)

      //send the email
      val future = mailer(envelope)

      //todo what happens if it can't send? maybe a Try block and drop a problem
      val result: Unit = Await.result(future, 60.seconds)
    }
  }
}