package net.shrine.email

import courier._
import courier.Defaults.executionContext
import net.shrine.util.ShouldMatchersForJUnit
import org.jvnet.mock_javamail.Mailbox
import javax.mail.internet.InternetAddress

import courier.Session.Builder
import org.junit.Test

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.implicitConversions

/**
  * Source of typesafe config for the problems database
  *
  * @author david
  * @since 1.22
  */
class EmailTest extends ShouldMatchersForJUnit {

  implicit def stringToInternetAddress(string:String):InternetAddress = new InternetAddress(string)

  @Test
  def testSendMockedEmail(): Unit = {

    val fromAddr:InternetAddress = new InternetAddress("someone@example.com")
    val envelope:Envelope = Envelope.from("someone@example.com").to("mom@gmail.com").cc("dad@gmail.com").subject("miss you").content(Text("hi mom"))

    val mailer: Mailer = Mailer("localhost", 25)()
    val future = mailer(envelope)

    Await.ready(future, 5.seconds)

    val momsInbox = Mailbox.get("mom@gmail.com")
    momsInbox.size === 1
    val momsMsg = momsInbox.get(0)
    momsMsg.getContent === "hi mom"
    momsMsg.getSubject === "miss you"

  }

}
