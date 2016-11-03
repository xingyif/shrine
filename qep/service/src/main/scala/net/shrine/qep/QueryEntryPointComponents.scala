package net.shrine.qep

import com.typesafe.config.Config
import net.shrine.authentication.{AuthenticationType, Authenticator, PmAuthenticator}
import net.shrine.authorization.{AllowsAllAuthorizationService, AuthorizationType, QueryAuthorizationService, StewardQueryAuthorizationService}
import net.shrine.broadcaster.dao.HubDao
import net.shrine.broadcaster.{BroadcastAndAggregationService, NodeHandle, SigningBroadcastAndAggregationService}
import net.shrine.client.Poster
import net.shrine.config.ConfigExtensions
import net.shrine.crypto.KeyStoreCertCollection
import net.shrine.crypto2.BouncyKeyStoreCollection
import net.shrine.dao.squeryl.SquerylInitializer
import net.shrine.hms.authentication.EcommonsPmAuthenticator
import net.shrine.hms.authorization.HmsDataStewardAuthorizationService
import net.shrine.log.Loggable
import net.shrine.protocol.ResultOutputType
import net.shrine.qep.dao.AuditDao
import net.shrine.qep.dao.squeryl.SquerylAuditDao
import net.shrine.qep.dao.squeryl.tables.Tables
import net.shrine.util.{PeerToPeerModel, SingleHubModel, TrustModel}

import scala.util.Try

/**
  * @author david 
  * @since 1.22
  */
case class QueryEntryPointComponents(shrineService: QepService,
                                     i2b2Service: I2b2QepService,
                                     auditDao: AuditDao,  //todo auditDao is only used by the happy service to grab the most recent entries
                                     trustModel: Option[TrustModel]
                                    )

object QueryEntryPointComponents extends Loggable {
  def apply(
             qepConfig:Config,
             certCollection: BouncyKeyStoreCollection,
             breakdownTypes: Set[ResultOutputType],
             broadcastDestinations: Option[Set[NodeHandle]],
             hubDao: HubDao, //todo the QEP should not need the hub dao
             squerylInitializer: SquerylInitializer, //todo could really have its own
             pmPoster: Poster //todo could really have its own
           ):QueryEntryPointComponents = {

    val commonName: String = certCollection.myEntry.commonName.getOrElse {
      val hostname = java.net.InetAddress.getLocalHost.getHostName
      warn(s"No common name available from ${certCollection.myEntry}. Using $hostname instead.")
      hostname
    }

    val broadcastService: BroadcastAndAggregationService = SigningBroadcastAndAggregationService(
      qepConfig,
      certCollection,
      breakdownTypes,
      broadcastDestinations,
      hubDao //todo the QEP should not need the hub dao
    )

    val auditDao: AuditDao = new SquerylAuditDao(squerylInitializer, new Tables)
    val authenticator: Authenticator = AuthStrategy.determineAuthenticator(qepConfig, pmPoster)
    val authorizationService: QueryAuthorizationService = AuthStrategy.determineQueryAuthorizationService(qepConfig,authenticator)

    debug(s"authorizationService set to $authorizationService")

    QueryEntryPointComponents(
      QepService(
        qepConfig,
        commonName,
        auditDao,
        authenticator,
        authorizationService,
        broadcastService,
        breakdownTypes
      ),
      I2b2QepService(
        qepConfig,
        commonName,
        auditDao,
        authenticator,
        authorizationService,
        broadcastService,
        breakdownTypes
      ),
      auditDao,
      Try(qepConfig.getBoolean("trustModelIsHub")).toOption.map(if(_) SingleHubModel else PeerToPeerModel)
    )
  }
}

/**
  * @author clint
  * @since Jul 1, 2014
  */
object AuthStrategy {

  import AuthenticationType._
  import AuthorizationType._

  def determineAuthenticator(qepConfig:Config, pmPoster: Poster): Authenticator = {

    //todo put these default values in reference.conf if you decide to use one
    val defaultAuthenticationType: AuthenticationType = AuthenticationType.Pm
    val authType = qepConfig.getOption("authenticationType",_.getString).flatMap(AuthenticationType.valueOf).getOrElse(defaultAuthenticationType)

    authType match {
      case NoAuthentication => AllowsAllAuthenticator
      case Pm => PmAuthenticator(pmPoster)
      case Ecommons => EcommonsPmAuthenticator(pmPoster)
      case _ => throw new IllegalArgumentException(s"Unknown authentication type '$authType'")
    }
  }

  def determineQueryAuthorizationService(qepConfig:Config, authenticator: Authenticator): QueryAuthorizationService = {

    val defaultAuthorizationType: AuthorizationType = AuthorizationType.NoAuthorization //todo should default to DSA in the reference.conf instead of being optional
    val authorizationType = qepConfig.getOption("authorizationType",_.getString).flatMap(AuthorizationType.valueOf).getOrElse(defaultAuthorizationType)

    authorizationType match {
      case ShrineSteward => StewardQueryAuthorizationService(qepConfig.getConfig("shrineSteward"))
      case HmsSteward => HmsDataStewardAuthorizationService(qepConfig,authenticator)
      case NoAuthorization => AllowsAllAuthorizationService
      case _ => throw new IllegalArgumentException(s"Unknown authorization type '$authorizationType'")
    }
  }
}