package net.shrine.adapter.translators

import net.shrine.config.mappings.AdapterMappings
import net.shrine.log.Loggable
import net.shrine.protocol.query.Expression
import scala.util.Try

/**
 * @author Clint Gilbert
 * @since Feb 29, 2012
 *
 * A class to translate query Expressions from Shrine terms to local terms
 * given an AdapterMappings instance.
 */
object ExpressionTranslator {
  def apply(adapterMappings: AdapterMappings): ExpressionTranslator = {
    val mappings = Map.empty ++ (for {
      networkTerm <- adapterMappings.networkTerms
      localTerms = adapterMappings.localTermsFor(networkTerm)
    } yield {
      (networkTerm, localTerms)
    })

    new ExpressionTranslator(mappings)
  }
}

final class ExpressionTranslator(private[translators] val mappings: Map[String, Set[String]]) extends Loggable {

  def translate(expr: Expression): Expression = tryTranslate(expr).get.normalize
  
  //NB: Package-private for testing
  private[translators] def tryTranslate(expr: Expression): Try[Expression] = {
    expr.translate(v => mappings.get(v).getOrElse(Set.empty))
  }
}