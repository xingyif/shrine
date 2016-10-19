package net.shrine.email

import java.util.Properties
import javax.mail.Session

import com.typesafe.config.Config
import courier.Mailer

/**
  * Creates a courier Mailer via shrine.conf, by pulling out all possible properties from https://www.tutorialspoint.com/javamail_api/javamail_api_smtp_servers.htm from an email section of shrine.conf
  *
  * @author david 
  * @since 1.22
  */
object ConfiguredMailer {

  def createMailerFromConfig(config:Config):Mailer = {
    //First convert the config to a java.util.Properties
    import scala.collection.JavaConversions._

    val properties = new Properties()

    val map: Map[String, Object] = config.entrySet().map({ entry =>
      entry.getKey -> entry.getValue.unwrapped()
    })(collection.breakOut)

    properties.putAll(map)

    println(s"properties is $properties")

    //Then make the session
    val session = Session.getDefaultInstance(properties)

    //And finally the mailer
    Mailer(session) //todo username and password for gmail
  }
}
