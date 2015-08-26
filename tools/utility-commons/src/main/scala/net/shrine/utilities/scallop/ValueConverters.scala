package net.shrine.utilities.scallop

import scala.concurrent.duration.Duration
import scala.util.Try
import org.rogach.scallop.ValueConverter
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import java.io.File

/**
 * @author clint
 * @date Oct 17, 2013
 */
object ValueConverters {
  object Implicits {
    implicit val fileValueConverter: ValueConverter[File] = new SimpleManifestValueConverter(new File(_))
    
    implicit val durationValueConverter: ValueConverter[Duration] = new ManifestValueConverter[Duration] {
      override val parseFirst: Parser[Duration] = {
        case (_, Seq(howManyString, timeUnitString)) => Right {
          (for {
            howMany <- Try(howManyString.toInt)
            duration <- durationFrom(howMany, timeUnitString)
          } yield duration).toOption
        }
      }
    }

    implicit val authnValueConverter: ValueConverter[AuthenticationInfo] = new ManifestValueConverter[AuthenticationInfo] {
      override val parseFirst: Parser[AuthenticationInfo] = {
        case (_, Seq(domain, username, password)) => Right {
          Option(authInfoFrom(domain, username, password))
        }
      }
    }
  }

  def durationFrom(magnitude: Int, timeUnit: String): Try[Duration] = Try {
    import scala.concurrent.duration._

    timeUnit match {
      case Keys.`milliseconds` => magnitude.milliseconds
      case Keys.`seconds` => magnitude.seconds
      case Keys.`minutes` => magnitude.minutes
      case _ => throw new IllegalArgumentException(s"Unhandled time unit '$timeUnit'")
    }
  }

  def authInfoFrom(domain: String, username: String, password: String): AuthenticationInfo = {
    AuthenticationInfo(domain, username, Credential(password, false))
  }
}