package net.shrine.utilities.batchquerier.components

import org.junit.Test
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import java.io.File
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author clint
 * @date Oct 18, 2013
 */
final class BatchQuerierConfigComponentTest extends ShouldMatchersForJUnit {
  @Test
  def testInit {
    final case class MockComponent(override val args: Seq[String]) extends BatchQuerierConfigComponent

    //application.conf only
    {
      val component = MockComponent(Nil)

      val config = component.config

      config should not be (null)

      config.authorization should equal(AuthenticationInfo("ExampleDomain", "ExampleUser", Credential("ExamplePassword", false)))
      config.expressionFile should equal(new File("/path/to/my/queries.xml"))
      config.outputFile should equal(new File("/path/to/desired/output.csv"))
      config.projectId should equal("SHRINE")
      config.queriesPerTerm should equal(99)
      config.shrineUrl should equal("https://some-shrine-node.example.com:6060/shrine-cell/rest/")
      config.topicId should equal("some-topic-id")
    }

    //Override application.conf with command-line args
    {
      val url = "http://example.com/shrine"
      val authn = AuthenticationInfo("FooDomain", "FooUser", Credential("FooPassword", false))
      val inputFileName = "foo.xml"
      val outputFileName = "foo.csv"
      val projectId = "foo"
      val topicId = "bar"
      val queriesPerTerm = 123

      val args = Seq(
        "-i", inputFileName,
        "-u", url,
        "-c", authn.domain, authn.username, authn.credential.value,
        "-o", outputFileName,
        "-p", projectId,
        "-t", topicId,
        "-n", queriesPerTerm.toString)

      val component = MockComponent(args)

      val config = component.config

      config should not be (null)

      config.authorization should equal(authn)
      config.expressionFile should equal(new File(inputFileName))
      config.outputFile should equal(new File(outputFileName))
      config.projectId should equal(projectId)
      config.queriesPerTerm should equal(queriesPerTerm)
      config.shrineUrl should equal(url)
      config.topicId should equal(topicId)
    }
  }
}