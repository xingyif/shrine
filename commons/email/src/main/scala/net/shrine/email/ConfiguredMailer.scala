package net.shrine.email

import java.util.Properties
import javax.mail.{Authenticator, PasswordAuthentication, Session}

import com.typesafe.config.Config
import courier.Mailer
import net.shrine.config.ConfigExtensions

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

    val map: Map[String, Object] = config.getConfig("javaxmail").entrySet().map({ entry =>
      entry.getKey -> entry.getValue.unwrapped()
    })(collection.breakOut)

    properties.putAll(map)

    def authenticatorFromConfig(config: Config) = {
      new javax.mail.Authenticator() {
        override def getPasswordAuthentication() = new PasswordAuthentication(config.getString("username"), config.getString("password"))
      }
    }

    val configAuthenticator = config.getOptionConfigured("authenticator",authenticatorFromConfig)

    //Then make the session
    val session = configAuthenticator.fold(Session.getDefaultInstance(properties))(
                    authenticator => Session.getDefaultInstance(properties,authenticator))

    //And finally the mailer
    Mailer(session)
  }
}
