package net.shrine.utilities.batchquerier.components

import org.junit.Test
import net.shrine.utilities.commands.CompoundCommand
import net.shrine.utilities.commands.CompoundCommand
import net.shrine.utilities.batchquerier.commands.ReadXmlQueryDefs
import net.shrine.utilities.batchquerier.commands.QueryWith
import net.shrine.utilities.commands.WriteTo
import net.shrine.utilities.batchquerier.commands.ToCsv
import net.shrine.utilities.batchquerier.commands.FormatForOutput
import net.shrine.utilities.batchquerier.commands.GroupRepeated
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author clint
 * @date Oct 4, 2013
 */
final class BatchQuerierModuleTest extends ShouldMatchersForJUnit {
  @Test
  def testQuery {
    val batchQuerier = new BatchQuerierModule(Nil)
    
    val command = batchQuerier.query
    
    val CompoundCommand(CompoundCommand(CompoundCommand(CompoundCommand(CompoundCommand(step1, step2), step3), step4), step5), step6) = command

    step1 should be(ReadXmlQueryDefs)
    
    step2.asInstanceOf[QueryWith].querier should be(batchQuerier)
    
    step3 should be(GroupRepeated)
    
    step4 should be(FormatForOutput)
    
    step5 should be(ToCsv)
    
    step6.asInstanceOf[WriteTo].file should equal(batchQuerier.config.outputFile)
  }
}