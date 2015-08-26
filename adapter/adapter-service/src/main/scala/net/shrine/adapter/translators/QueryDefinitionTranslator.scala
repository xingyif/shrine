package net.shrine.adapter.translators

import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.protocol.query.Expression

/**
 * @author Clint Gilbert
 * @date Feb 29, 2012
 * 
 * Convenience class to translate QueryDefinitions' wrapped Expressions 
 * from Shrine terms to local terms given an AdapterMappings instance
 */
final class QueryDefinitionTranslator(private[translators] val expressionTranslator: ExpressionTranslator) {
  
  def translate(queryDef: QueryDefinition) = queryDef.transform(expressionTranslator.translate)
  
}