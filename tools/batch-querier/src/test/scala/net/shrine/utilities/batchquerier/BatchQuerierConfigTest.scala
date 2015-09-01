package net.shrine.utilities.batchquerier

import org.junit.Test
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValue
import com.typesafe.config.impl.ConfigBoolean
import java.io.File
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author clint
 * @since Sep 9, 2013
 */
final class BatchQuerierConfigTest extends ShouldMatchersForJUnit {
  import BatchQuerierConfigTest._

  @Test
  def testApply {
    import BatchQuerierConfig.BatchQuerierConfigKeys

    val domain = "some-domain"
    val username = "some-username"
    val password = "some-password"
    val projectId = "SHRINE"
    val shrineUrl = "http://example.com"
    val outputFile = "output.csv"
    val inputFile = "input.xml"
    val topicId = "some-topic-id"
    val topicName = "some-topic-name"

    import net.shrine.utilities.scallop.{Keys => BaseKeys}
      
    val config = {
      import scala.collection.JavaConverters._
      
      //NB: Intentionally omit queriesPerTerm, to see if the default is chosen
      Seq(
        BatchQuerierConfigKeys.credentials -> Map(BaseKeys.domain -> domain, BaseKeys.username -> username, BaseKeys.password -> password).asJava,
        BatchQuerierConfigKeys.projectId -> projectId,
        BatchQuerierConfigKeys.shrineUrl -> shrineUrl,
        BatchQuerierConfigKeys.outputFile -> outputFile,
        BatchQuerierConfigKeys.inputFile -> inputFile,
        BatchQuerierConfigKeys.topicId -> topicId,
        BatchQuerierConfigKeys.topicName -> topicName).asConfig
    }
    
    //Should parse
    {
      val batchConfig = BatchQuerierConfig(config)

      batchConfig.authorization.domain should equal(domain)
      batchConfig.authorization.username should equal(username)
      batchConfig.authorization.credential.value should equal(password)
      batchConfig.projectId should equal(projectId)
      batchConfig.shrineUrl should equal(shrineUrl)
      batchConfig.outputFile should equal(new File(outputFile))
      batchConfig.expressionFile should equal(new File(inputFile))
      batchConfig.topicId should equal(topicId)
      batchConfig.queriesPerTerm should equal(BatchQuerierConfig.Defaults.queriesPerTerm)
    }

    //Should not parse if anything else is missing
    
    intercept[Exception] {
      BatchQuerierConfig(ConfigFactory.empty)
    }
    
    intercept[Exception] {
      BatchQuerierConfig(config.withoutPath(BatchQuerierConfigKeys.credentials))
    }

    intercept[Exception] {
      BatchQuerierConfig(config.withoutPath(s"${BatchQuerierConfigKeys.credentials}.${BaseKeys.domain}"))
    }

    intercept[Exception] {
      BatchQuerierConfig(config.withoutPath(s"${BatchQuerierConfigKeys.credentials}.${BaseKeys.username}"))
    }

    intercept[Exception] {
      BatchQuerierConfig(config.withoutPath(s"${BatchQuerierConfigKeys.credentials}.${BaseKeys.password}"))
    }

    intercept[Exception] {
      BatchQuerierConfig(config.withoutPath(BatchQuerierConfigKeys.projectId))
    }

    intercept[Exception] {
      BatchQuerierConfig(config.withoutPath(BatchQuerierConfigKeys.shrineUrl))
    }

    intercept[Exception] {
      BatchQuerierConfig(config.withoutPath(BatchQuerierConfigKeys.outputFile))
    }

    intercept[Exception] {
      BatchQuerierConfig(config.withoutPath(BatchQuerierConfigKeys.inputFile))
    }
    
    intercept[Exception] {
      BatchQuerierConfig(config.withoutPath(BatchQuerierConfigKeys.topicId))
    }
  }
}

object BatchQuerierConfigTest {
  private implicit class TupleToConfig(val tuples: Seq[(String, Any)]) extends AnyVal {
    def asConfig: Config = {
      import scala.collection.JavaConverters._

      ConfigFactory.parseMap(tuples.toMap.asJava)
    }
  }
}