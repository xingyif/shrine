package net.shrine.crypto

import com.typesafe.config.ConfigFactory
import net.shrine.util.ShouldMatchersForJUnit
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
      val descriptor = KeyStoreDescriptorParser(ConfigFactory.parseString("""
          file="foo"
          password="bar"
          privateKeyAlias="baz"
          keyStoreType="jks"
          caCertAliases = [foo, bar]
          """))
          
      descriptor.file should be("foo")
      descriptor.password should be("bar")
      descriptor.privateKeyAlias should be(Some("baz"))
      descriptor.keyStoreType should be(KeyStoreType.JKS)
      descriptor.caCertAliases.toSet should be(Set("foo", "bar"))
    }
    
    //All fields, PKCS12
    {
      val descriptor = KeyStoreDescriptorParser(ConfigFactory.parseString("""
          file="foo"
          password="bar"
          privateKeyAlias="baz"
          keyStoreType="pkcs12"
          """))
          
      descriptor.file should be("foo")
      descriptor.password should be("bar")
      descriptor.privateKeyAlias should be(Some("baz"))
      descriptor.keyStoreType should be(KeyStoreType.PKCS12)
    }
    
    //no keystore type
    {
      val descriptor = KeyStoreDescriptorParser(ConfigFactory.parseString("""
          file="foo"
          password="bar"
          privateKeyAlias="baz"
          """))
          
      descriptor.file should be("foo")
      descriptor.password should be("bar")
      descriptor.privateKeyAlias should be(Some("baz"))
      descriptor.keyStoreType should be(KeyStoreType.Default)
    }
    
    //no private key alias
    {
      val descriptor = KeyStoreDescriptorParser(ConfigFactory.parseString("""
          file="foo"
          password="bar"
          keyStoreType="jks"
          """))
          
      descriptor.file should be("foo")
      descriptor.password should be("bar")
      descriptor.privateKeyAlias should be(None)
      descriptor.keyStoreType should be(KeyStoreType.JKS)
    }

    //No file
    intercept[Exception] {
      KeyStoreDescriptorParser(ConfigFactory.parseString(""" password="bar" """))
    }
    
    //No password
    intercept[Exception] {
      KeyStoreDescriptorParser(ConfigFactory.parseString(""" file="foo" """))
    }
  }
}