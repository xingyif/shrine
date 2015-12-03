package net.shrine.utilities.batchquerier.commands

import org.junit.Test
import net.shrine.utilities.batchquerier.BatchQueryResult
import net.shrine.utilities.batchquerier.RepeatedBatchQueryResult
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.protocol.QueryResult
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author clint
 * @date Oct 11, 2013
 */
final class GroupRepeatedTest extends ShouldMatchersForJUnit {
  @Test
  def testApply {
    GroupRepeated(Nil) should be(Nil)

    import scala.concurrent.duration._

    val q1 = QueryDefinition("foo", Term("foo"))
    val q2 = QueryDefinition("bar", Term("bar"))
    val q3 = QueryDefinition("baz", Term("baz"))
    val q4 = QueryDefinition("blerg", Term("blerg"))

    val inst1 = "Foo"
    val inst2 = "Bar"
    val inst3 = "Baz"

    val q1i1Avg = (4.0 / 3.0).seconds.toMillis.milliseconds
    val q1i2Avg = (7.0 / 3.0).seconds.toMillis.milliseconds

    val q2i1Avg = 1.seconds
    val q2i2Avg = 2.seconds
    val q2i3Avg = 3.seconds

    val q3i3Avg = 99.567.minutes
    
    val q4i1Avg = 0.milliseconds

    import QueryResult.StatusType.{Finished, Error}
    
    val results = Seq(
      BatchQueryResult(inst1, q1, Finished, 1.second, 123L),
      BatchQueryResult(inst1, q1, Finished, 2.second, 123L),
      BatchQueryResult(inst1, q1, Finished, 1.seconds, 123L),
      
      BatchQueryResult(inst2, q1, Finished, 2.second, 123L),
      BatchQueryResult(inst2, q1, Finished, 2.seconds, 123L),
      BatchQueryResult(inst2, q1, Finished, 3.second, 123L),

      BatchQueryResult(inst1, q2, Finished, 1.seconds, 456L),
      BatchQueryResult(inst1, q2, Finished, 1.seconds, 456L),
      
      BatchQueryResult(inst2, q2, Finished, 2.seconds, 456L),
      BatchQueryResult(inst2, q2, Finished, 2.seconds, 456L),
      
      BatchQueryResult(inst3, q2, Finished, 3.seconds, 456L),
      BatchQueryResult(inst3, q2, Finished, 3.seconds, 456L),

      BatchQueryResult(inst3, q3, Finished, 99.567.minutes, 789L),
      
      BatchQueryResult(inst1, q4, Error, q4i1Avg, -1))

    val expected = Set(
      RepeatedBatchQueryResult(inst1, q1, Finished, 1.seconds, 123L, 3, q1i1Avg),
      RepeatedBatchQueryResult(inst2, q1, Finished, 2.seconds, 123L, 3, q1i2Avg),
      RepeatedBatchQueryResult(inst1, q1, Finished, 1.seconds, 123L, 3, q1i1Avg),
      RepeatedBatchQueryResult(inst2, q1, Finished, 2.seconds, 123L, 3, q1i2Avg),
      RepeatedBatchQueryResult(inst1, q1, Finished, 2.seconds, 123L, 3, q1i1Avg),
      RepeatedBatchQueryResult(inst2, q1, Finished, 3.seconds, 123L, 3, q1i2Avg),

      RepeatedBatchQueryResult(inst1, q2, Finished, 1.seconds, 456L, 2, q2i1Avg),
      RepeatedBatchQueryResult(inst2, q2, Finished, 2.seconds, 456L, 2, q2i2Avg),
      RepeatedBatchQueryResult(inst3, q2, Finished, 3.seconds, 456L, 2, q2i3Avg),
      RepeatedBatchQueryResult(inst1, q2, Finished, 1.seconds, 456L, 2, q2i1Avg),
      RepeatedBatchQueryResult(inst2, q2, Finished, 2.seconds, 456L, 2, q2i2Avg),
      RepeatedBatchQueryResult(inst3, q2, Finished, 3.seconds, 456L, 2, q2i3Avg),

      RepeatedBatchQueryResult(inst3, q3, Finished, 99.567.minutes, 789L, 1, q3i3Avg),
      
      RepeatedBatchQueryResult(inst1, q4, Error, q4i1Avg, -1, 1, q4i1Avg))

    //NB: Use toSet to disregard order
    GroupRepeated(results).toSet should equal(expected)
  }
}