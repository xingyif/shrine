package net.shrine.adapter.translators

import net.shrine.adapter.AdapterMappingException
import net.shrine.config.mappings.AdapterMappings
import net.shrine.log.Loggable
import net.shrine.protocol.query.And
import net.shrine.protocol.query.DateBounded
import net.shrine.protocol.query.Expression
import net.shrine.protocol.query.MappableExpression
import net.shrine.protocol.query.Not
import net.shrine.protocol.query.OccuranceLimited
import net.shrine.protocol.query.Or
import net.shrine.protocol.query.Term
import net.shrine.protocol.query.HasSingleSubExpression
import net.shrine.protocol.query.Constrained
import net.shrine.protocol.query.Modifiers
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import net.shrine.util.XmlDateHelper
import net.shrine.protocol.query.OccuranceLimited
import net.shrine.protocol.query.OccuranceLimited
import net.shrine.protocol.query.WithTiming

/**
 * @author Clint Gilbert
 * @date Feb 29, 2012
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