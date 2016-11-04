package net.shrine.steward.email

import java.util.Date
import javax.mail.internet.InternetAddress

import akka.actor.Actor
import com.typesafe.config.Config
import courier.{Envelope, Mailer, Text}
import net.shrine.authorization.steward.ResearcherToAudit
import net.shrine.config.{ConfigExtensions, DurationConfigParser}
import net.shrine.email.ConfiguredMailer
import net.shrine.log.Log
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.steward.StewardConfigSource
import net.shrine.steward.db.StewardDatabase

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.blocking
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.xml.NodeSeq

/**
  * @author david 
  * @since 1.22
  */
case class AuditEmailer(maxQueryCountBetweenAudits:Int,
                        minTimeBetweenAudits:FiniteDuration,
                        researcherLineTemplate:String,
                        emailTemplate:String,
                        emailSubject:String,
                        from:InternetAddress,
                        to:InternetAddress,
                        stewardBaseUrl:Option[String],
                        mailer:Mailer
                       ) {
  def audit() = {
    //gather a list of users to audit
    val now = System.currentTimeMillis()
    val researchersToAudit: Seq[ResearcherToAudit] = StewardDatabase.db.selectResearchersToAudit(maxQueryCountBetweenAudits,
      minTimeBetweenAudits,
      now)
    if (researchersToAudit.nonEmpty){

      val auditLines = researchersToAudit.sortBy(_.count).reverse.map { researcher =>
        researcherLineTemplate.replaceAll("FULLNAME",researcher.researcher.fullName)
          .replaceAll("USERNAME",researcher.researcher.userName)
          .replaceAll("COUNT",researcher.count.toString)
          .replaceAll("LAST_AUDIT_DATE",new Date(researcher.leastRecentQueryDate).toString)
      }.mkString("\n")

      //build up the email body
      val withLines = emailTemplate.replaceAll("AUDIT_LINES",auditLines)
      val withBaseUrl = stewardBaseUrl.fold(withLines)(withLines.replaceAll("STEWARD_BASE_URL",_))
      val emailBody = Text(withBaseUrl)

      val envelope:Envelope = Envelope.from(from).to(to).subject(emailSubject).content(emailBody)

      Log.debug(s"About to send $envelope .")

      //send the email
      val future = mailer(envelope)

      try {
        blocking {
          Await.result(future, 60.seconds)
        }
        StewardDatabase.db.logAuditRequests(researchersToAudit, now)
        Log.info(s"Sent and logged $envelope .")
      } catch {
        case NonFatal(x) => CouldNotSendAuditEmail(envelope,x)
      }
    }
  }
}

object AuditEmailer {

  /**
    *
    * @param config All of shrine.conf
    */
  def apply(config:Config):AuditEmailer = {
    val config = StewardConfigSource.config
    val emailConfig = config.getConfig("shrine.steward.emailDataSteward")

    AuditEmailer(
      maxQueryCountBetweenAudits = emailConfig.getInt("maxQueryCountBetweenAudits"),
      minTimeBetweenAudits = emailConfig.get("minTimeBetweenAudits", DurationConfigParser.parseDuration),
      researcherLineTemplate = emailConfig.getString("researcherLine"),
      emailTemplate = emailConfig.getString("emailBody"),
      emailSubject = emailConfig.getString("subject"),
      from = emailConfig.get("from", new InternetAddress(_)),
      to = emailConfig.get("to", new InternetAddress(_)),
      stewardBaseUrl = config.getOption("stewardBaseUrl", _.getString),
      mailer = ConfiguredMailer.createMailerFromConfig(config.getConfig("shrine.email")))
  }

  /**
    * Check the emailer's config, log any problems
    *
    * @param config All of shrine.conf
    */
  def configCheck(config:Config):Boolean = try {
    val autoEmailer = apply(config)
    Log.info(s"DSA will request audits from ${autoEmailer.to}")
    true
  } catch {
    case NonFatal(x) =>
      CannotConfigureAuditEmailer(x)
      false
  }
}

class AuditEmailerActor extends Actor {

  override def receive: Receive = {case _ =>
    val config = StewardConfigSource.config
    AuditEmailer(config).audit()
  }
}

case class CannotConfigureAuditEmailer(ex:Throwable) extends AbstractProblem(ProblemSources.Dsa) {
  override def summary: String = "The DSA will not email audit requests due to a misconfiguration."

  override def description: String = s"The DSA will not email audit requests due to ${throwable.get}"

  override def throwable = Some(ex)
}

case class CouldNotSendAuditEmail(envelope:Envelope,ex:Throwable) extends AbstractProblem(ProblemSources.Dsa) {
  override def summary: String = "The DSA was not able to send an audit email."

  override def description: String = s"The DSA was not able to send an audit request to ${envelope.to} due to ${throwable.get}"

  override def throwable = Some(ex)

  override def detailsXml:NodeSeq = <details>
    {s"Could not send $envelope"}
    {throwableDetail}
  </details>
}