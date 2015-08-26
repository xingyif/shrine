package net.shrine.util

import org.junit.Test
import scala.xml.Elem
import scala.xml.NodeSeq

/**
 * @author clint
 * @date Sep 30, 2014
 */
final class OptionEnrichmentsTest extends ShouldMatchersForJUnit {
  import OptionEnrichments._

  private val none: Option[Int] = None
  private val nullElem: Elem = null
  private val nullString: String = null
  private def nullFn[T]: T => NodeSeq = null

  private final class Nuh(x: Int) {
    override def toString = s"xyz: $x"
    
    def toXml: NodeSeq = <baz>{ x }</baz>
  }

  @Test
  def testToXmlElem: Unit = {
    none.toXml(nullElem) should be(null)
    none.toXml(<foo/>) should be(null)

    Some(123).toXml(<foo/>) should be(<foo>123</foo>)

    Some(new Nuh(123)).toXml(<bar></bar>) should be(<bar>xyz: 123</bar>)
  }
  
  @Test
  def testToXmlFn: Unit = {
    none.toXml(nullFn) should be(null)
    none.asInstanceOf[Option[Nuh]].toXml(_.toXml) should be(null)

    Some(new Nuh(123)).toXml(_.toXml).toString should be(<baz>123</baz>.toString)
  }
  
  @Test
  def testToXmlElemAndFn: Unit = {
    none.toXml(<foo/>, nullFn) should be(null)
    none.asInstanceOf[Option[Nuh]].toXml(<foo/>, _.toXml) should be(null)

    Some(new Nuh(123)).toXml(<foo/>, _.toXml).toString should be(<foo><baz>123</baz></foo>.toString)
  }
}