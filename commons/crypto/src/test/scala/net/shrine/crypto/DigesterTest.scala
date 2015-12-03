package net.shrine.crypto

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.serialization.XmlMarshaller

/**
 * @author clint
 * @date Dec 2, 2013
 */
final class DigesterTest extends ShouldMatchersForJUnit {
  @Test
  def testXmlMarshallersAreDigestable {
    final class Foo(x: Int) extends XmlMarshaller {
      override def toXml = <foo>{ x }</foo>
    }
    
    def digest[T : Digester](t: T): Array[Byte] = implicitly[Digester[T]].digest(t)
    
    val foo = new Foo(123)
    
    digest(foo) should equal(foo.toXmlString.getBytes("UTF-8"))
  }
}