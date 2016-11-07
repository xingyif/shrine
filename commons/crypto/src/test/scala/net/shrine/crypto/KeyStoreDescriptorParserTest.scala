package net.shrine.crypto

import com.typesafe.config.ConfigFactory
import net.shrine.util.{PeerToPeerModel, ShouldMatchersForJUnit, SingleHubModel}
import org.junit.Test

/**
 * @author clint
 * @since Dec 11, 2013
 */
final class KeyStoreDescriptorParserTest extends ShouldMatchersForJUnit {
  @Test
  def testApply {
    //All fields, JKS
    {
      val descriptor = KeyStoreDescriptorParser(
        ConfigFactory.parseString("""
                file="foo"
                password="bar"
                privateKeyAlias="baz"
                keyStoreType="jks"
                caCertAliases = [foo, bar]
                trustModelIsHub = true
                aliasMap = {
                  site1 = "downstream1"
                }
                """),
        ConfigFactory.parseString(
          """
            |downstreamNodes {
            |  site1 = "https://localhost:8080/shrine/"
            |}
          """.stripMargin),
        ConfigFactory.empty()
      )
          
      descriptor.file should be("foo")
      descriptor.password should be("bar")
      descriptor.privateKeyAlias should be(Some("baz"))
      descriptor.keyStoreType should be(KeyStoreType.JKS)
      descriptor.caCertAliases.toSet should be(Set("foo", "bar"))
      descriptor.trustModel should be(SingleHubModel(true))
      descriptor.remoteSiteDescriptors should be(Seq(RemoteSiteDescriptor("site1", "downstream1", "localhost")))
    }
    
    //All fields, PKCS12
    {
      val descriptor = KeyStoreDescriptorParser(
        ConfigFactory.parseString("""
                file="foo"
                password="bar"
                privateKeyAlias="baz"
                keyStoreType="pkcs12"
                trustModelIsHub = true
                aliasMap = {
                  hub = "carra"
                }
                """),
        ConfigFactory.empty(),
        ConfigFactory.parseString(
          """
            |broadcasterServiceEndpoint {
            |  url = "https://localhost:8080/shrine/"
            |}
          """.stripMargin)
      )
          
      descriptor.file should be("foo")
      descriptor.password should be("bar")
      descriptor.privateKeyAlias should be(Some("baz"))
      descriptor.keyStoreType should be(KeyStoreType.PKCS12)
      descriptor.trustModel should be(SingleHubModel(false))
      descriptor.remoteSiteDescriptors should be(Seq(RemoteSiteDescriptor("hub", "carra", "localhost")))
    }
    
    //no keystore type
    {
      val descriptor = KeyStoreDescriptorParser(
        ConfigFactory.parseString("""
                file="foo"
                password="bar"
                privateKeyAlias="baz"
                trustModelIsHub = true
                isHub = true
                aliasMap = {
                  site1 = "downstream1"
                  site2 = "downstream2"
                }
                """),
        ConfigFactory.parseString(
          """
            |downstreamNodes = {
            |  site1 = "https://someRemoteSite:7777/shrine/test"
            |  site2 = "https://someOtherSite:8888/shrine/test"
            |}
          """.stripMargin),
        ConfigFactory.empty()
      )
          
      descriptor.file should be("foo")
      descriptor.password should be("bar")
      descriptor.privateKeyAlias should be(Some("baz"))
      descriptor.keyStoreType should be(KeyStoreType.Default)
      descriptor.trustModel should be (SingleHubModel(true))
      descriptor.remoteSiteDescriptors should contain theSameElementsAs Seq(
        RemoteSiteDescriptor("site1", "downstream1", "someRemoteSite"),
        RemoteSiteDescriptor("site2", "downstream2", "someOtherSite"))
    }
    
    //no private key alias
    {
      val descriptor = KeyStoreDescriptorParser(
        ConfigFactory.parseString("""
                file="foo"
                password="bar"
                keyStoreType="jks"
                trustModelIsHub = false
                aliasMap = {
                  site1 = "node1"
                }
                """),
        ConfigFactory.parseString(
          """
            |downstreamNodes = {
            |  site1 = "https://somePeerSite:9999/shrine/blah"
            |}
          """.stripMargin),
        ConfigFactory.empty()
      )

      descriptor.file should be("foo")
      descriptor.password should be("bar")
      descriptor.privateKeyAlias should be(None)
      descriptor.trustModel should be(PeerToPeerModel)
      descriptor.keyStoreType should be(KeyStoreType.JKS)
      descriptor.remoteSiteDescriptors should be(Seq(RemoteSiteDescriptor("site1", "node1", "somePeerSite")))
    }

    //No file
    intercept[Exception] {
      KeyStoreDescriptorParser(ConfigFactory.parseString(""" password="bar" """), ConfigFactory.empty(), ConfigFactory.empty())
    }

    //No password
    intercept[Exception] {
      KeyStoreDescriptorParser(ConfigFactory.parseString(""" file="foo" """), ConfigFactory.empty(), ConfigFactory.empty())
    }

    //Alias size doesn't match up
    intercept[AssertionError] {
      {
        val descriptor = KeyStoreDescriptorParser(
          ConfigFactory.parseString("""
                file="foo"
                password="bar"
                privateKeyAlias="baz"
                trustModelIsHub = true
                isHub = true
                aliasMap = {
                  site1 = "downstream1"
                  site2 = "downstream2"
                }
                                    """),
          ConfigFactory.parseString(
            """
              |downstreamNodes = {
              |  site1 = "https://someRemoteSite:7777/shrine/test"
              |}
            """.stripMargin),
          ConfigFactory.empty()
        )
      }
    }

    //Names don't correspond
    intercept[AssertionError] {
      {
        val descriptor = KeyStoreDescriptorParser(
          ConfigFactory.parseString("""
                file="foo"
                password="bar"
                privateKeyAlias="baz"
                trustModelIsHub = true
                isHub = true
                aliasMap = {
                  site1 = "downstream1"
                  site2 = "downstream2"
                }
                                    """),
          ConfigFactory.parseString(
            """
              |downstreamNodes = {
              |  site1 = "https://someRemoteSite:7777/shrine/test"
              |  site3 = "https://someRemoteSite:7777/shrine/test"
              |}
            """.stripMargin),
          ConfigFactory.empty()
        )
      }
    }

    //Extraneous mappings
    intercept[AssertionError] {
      {
        val descriptor = KeyStoreDescriptorParser(
          ConfigFactory.parseString("""
                file="foo"
                password="bar"
                privateKeyAlias="baz"
                trustModelIsHub = true
                isHub = false
                aliasMap = {
                  site1 = "downstream1"
                  dontdothisman = "downstream2"
                }
                                    """),
          ConfigFactory.empty(),
          ConfigFactory.parseString(
            """
              |broadcasterServiceEndpoint {
              |  url = "https://localhost:8080/shrine/"
              |}
            """.stripMargin)
        )
      }
    }

  }
}