package net.shrine.utilities.batchquerier.commands

import org.junit.Test
import net.shrine.protocol.query.Term
import net.shrine.protocol.query.Or
import net.shrine.protocol.query.And
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import net.shrine.protocol.query.QueryDefinition
import net.shrine.util.XmlUtil
import scala.xml.Node
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author clint
 * @date Oct 8, 2013
 */
final class ReadXmlQueryDefsTest extends ShouldMatchersForJUnit {
  val e1 = Term("foo")
  val e2 = Term("bar")
  val e3 = Or(e1, e2)
  val e4 = And(e1, e3)
  
  val q1 = QueryDefinition("foo", e1)
  val q2 = QueryDefinition("bar", e2)
  val q3 = QueryDefinition("or", e3)
  val q4 = QueryDefinition("and", e4)

  val queries = Seq(q1, q2, q3, q4)
  
  @Test
  def testApply {
    intercept[Exception] {
      ReadXmlQueryDefs(new File("does-not-exist"))
    }
    
    withFile() { file =>
      ReadXmlQueryDefs(file).toSeq should equal(queries) 
    }
  }
  
  @Test
  def testApplyPrettyPrintedXml {
    withFile(XmlUtil.prettyPrint) { file =>
      ReadXmlQueryDefs(file).toSeq should equal(queries) 
    }
  }

  private def withFile(toString: Node => String = _.toString)(f: File => Unit){
    val file = new File("target/aslkdjalksdjlkasdjlasd")

    val xml = {
      <queryDefinitions>
        { queries.map(_.toXml).map(xml => XmlUtil.renameRootTag("queryDefinition")(xml.head)) }
      </queryDefinitions>
    }

    try {
      val out = new FileWriter(file)

      try {
        out.write(xml.toString)
      } finally {
        out.close()
      }
      
      f(file)
      
    } finally {
      file.delete()
      file.exists() should be(false)
    }
  }
}