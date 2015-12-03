package net.shrine.utilities.batchquerier.commands

import org.junit.Test
import net.shrine.utilities.batchquerier.BatchQueryResult
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.utilities.batchquerier.csv.CsvRow
import net.shrine.utilities.batchquerier.RepeatedBatchQueryResult
import net.shrine.protocol.QueryResult
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author clint
 * @date Oct 8, 2013
 */
final class FormatForOutputTest extends ShouldMatchersForJUnit {
  @Test
  def testApply {
    
    FormatForOutput.apply(Nil) should be(Nil)
    
    import scala.concurrent.duration._
    
    import QueryResult.StatusType.{Error, Finished}
    
    val results = Seq(RepeatedBatchQueryResult("Foo", QueryDefinition("foo", Term("foo")), Finished, 1.second, 123L, 2, (1.5).seconds),
                      RepeatedBatchQueryResult("Bar", QueryDefinition("bar", Term("bar")), Finished, 2.seconds, 456L, 2, 3.seconds ),
                      RepeatedBatchQueryResult("Baz", QueryDefinition("baz", Term("baz")), Error, 99.567.minutes, 789L, 1, 99.567.minutes))
    
    val expected = Seq(CsvRow("foo", "Foo", "Finished", 123L, "1.00", "1.50", 2, Term("foo").toXmlString),
                       CsvRow("bar", "Bar", "Finished", 456L, "2.00", "3.00", 2, Term("bar").toXmlString),
                       CsvRow("baz", "Baz", "Error", 789L, "5974.02", "5974.02", 1, Term("baz").toXmlString))
                      
    FormatForOutput.apply(results) should equal(expected) 
  }
  
  @Test
  def testToFormattedSeconds {
    import FormatForOutput.toFormattedSeconds
    
    import scala.concurrent.duration._
    
    //Note last digit is rounded up
    toFormattedSeconds(12345.milliseconds) should equal("12.35")
    toFormattedSeconds(0.milliseconds) should equal("0.00")
    toFormattedSeconds(123.milliseconds) should equal("0.12")
    toFormattedSeconds(1.2345.seconds) should equal("1.23")
  }
  
  @Test
  def testToTitleCase {
    import FormatForOutput.toTitleCase
    
    toTitleCase("") should equal("")
    toTitleCase("x") should equal("X")
    toTitleCase("x") should equal("X")
    toTitleCase("1") should equal("1")
    toTitleCase("hello") should equal("Hello")
    toTitleCase("FOO") should equal("Foo")
    toTitleCase("lAlAlA") should equal("Lalala")
  }
}