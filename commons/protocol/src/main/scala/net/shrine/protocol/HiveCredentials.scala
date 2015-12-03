package net.shrine.protocol

import com.typesafe.config.Config
import net.shrine.config.Keys

/**
 * @author Bill Simons
 * @since 3/12/12
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
final case class HiveCredentials(domain: String, username: String, password: String, projectId: String) {
  def toAuthenticationInfo = AuthenticationInfo(domain, username, Credential(password, isToken = false))
}

object HiveCredentials {

  sealed case class ProjectIdType(key:String)

  val CRC = ProjectIdType(Keys.crcProjectId)
  val ONT = ProjectIdType(Keys.ontProjectId)

  def apply(config:Config, projectIdType: ProjectIdType) = {
    val credentials = CredentialConfig(config)

    val projectId = config.getString(projectIdType.key)

    new HiveCredentials(credentials.domain.get, credentials.username, credentials.password, projectId)
  }
}