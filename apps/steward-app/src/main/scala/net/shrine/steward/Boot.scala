package net.shrine.steward

import akka.actor.{ActorSystem, Props}
import net.shrine.config.{ConfigExtensions, DurationConfigParser}
import net.shrine.log.Loggable
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.steward.db.StewardDatabase
import net.shrine.steward.email.{AuditEmailer, AuditEmailerActor}
import spray.servlet.WebBoot

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.control.NonFatal

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

  val emailConfig = config.getConfig("shrine.steward.emailDataSteward")

  if(emailConfig.getBoolean("sendAuditEmails") && AuditEmailer.configCheck(config)) {

    try {
      system.scheduler.schedule(initialDelay = 0 milliseconds, //todo figure out how to handle the initial delay
        interval = emailConfig.get("interval", DurationConfigParser.parseDuration),
        receiver = system.actorOf(Props[AuditEmailerActor]),
        "tick")
    }
    catch {
      case NonFatal(x)  => CannotStartAuditEmailActor(x)
      case x:ExceptionInInitializerError => CannotStartAuditEmailActor(x)
    }
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

case class CannotStartAuditEmailActor(ex:Throwable) extends AbstractProblem(ProblemSources.Dsa) {
  override def summary: String = "The DSA could not start an Actor to email audit requests due to an exception."

  override def description: String = s"The DSA will not email audit requests due to ${throwable.get}"

  override def throwable = Some(ex)
}

