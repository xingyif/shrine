package net.shrine.utilities.batchquerier.commands

import org.junit.Test
import net.shrine.utilities.batchquerier.csv.CsvRow
import net.shrine.protocol.query.Term
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author clint
 * @date Oct 8, 2013
 */
final class ToCsvTest extends ShouldMatchersForJUnit {
  @Test
  def testApply {
    val headerRow = """"Name","Institution","Status","Count","Elapsed In Seconds","Mean Elapsed In Seconds","Num Queries Performed","Expression Xml""""
    
    ToCsv.apply(Nil) should equal(headerRow)

    val rows = Seq(CsvRow("foo", "Foo", "Finished", 123L, "1.00", "1.50", 2, Term("foo").toXmlString),
                   CsvRow("bar", "Bar", "Finished", 456L, "2.00", "2.00", 2, Term("bar").toXmlString),
                   CsvRow("baz", "Baz", "Error", 789L, "5974.02", "5974.02", 1, Term("baz").toXmlString))
                   
    val csvString = ToCsv.apply(rows)
    
    val expectedRows = Seq(
        headerRow,
        """"foo","Foo","Finished","123","1.00","1.50","2","<term>foo</term>"""", 
        """"bar","Bar","Finished","456","2.00","2.00","2","<term>bar</term>"""", 
        """"baz","Baz","Error","789","5974.02","5974.02","1","<term>baz</term>"""")
      
    csvString should equal(expectedRows.mkString("\n"))
  }
}