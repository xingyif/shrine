package net.shrine.adapter.translators

import org.junit.Test

import net.shrine.config.mappings.AdapterMappings
import net.shrine.protocol.query.Expression
import net.shrine.protocol.query.I2b2QuerySpan
import net.shrine.protocol.query.I2b2SubQueryConstraint
import net.shrine.protocol.query.I2b2SubQueryConstraints
import net.shrine.protocol.query.MappingException
import net.shrine.protocol.query.Or
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.QueryTiming
import net.shrine.protocol.query.Term
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author Clint Gilbert
 * @date Mar 1, 2012
 *
 */
final class QueryDefinitionTranslatorTest extends ShouldMatchersForJUnit {
  private val localTerms = Set("localTerm1", "localTerm2")

  private val mappings = Map("twoMatches" -> localTerms, "oneMatch" -> Set("localTerm3"))

  private def queryDef(expr: Expression) = QueryDefinition("foo", expr)

  private def queryDefWithOptionalFields(expr: Expression): QueryDefinition = {
    val constraints = I2b2SubQueryConstraints(
      "some-op",
      I2b2SubQueryConstraint("x", "y", "z"),
      I2b2SubQueryConstraint("a", "b", "c"),
      Some(I2b2QuerySpan("asdasd", "asdjkjlsad", "sadlasd")))

    val subQueries = Seq(queryDef(expr), queryDef(expr))

    QueryDefinition("foo", Option(expr), Some(QueryTiming.SameVisit), Some("id"), Some("queryType"), Some(constraints), subQueries)
  }

  private val adapterMappings = AdapterMappings("test",mappings = mappings)

  @Test
  def testConstructor: Unit = {
    val translator = new QueryDefinitionTranslator(new ExpressionTranslator(mappings))

    translator.expressionTranslator.mappings should equal(mappings)
  }

  @Test
  def testTranslate: Unit = {
    val translator = new QueryDefinitionTranslator(new ExpressionTranslator(mappings))

    translator.translate(queryDef(Term("oneMatch"))) should equal(queryDef(Term("localTerm3")))
    translator.translate(queryDef(Term("twoMatches"))) should equal(queryDef(Or(Term("localTerm1"), Term("localTerm2"))))
  }
  
  @Test
  def testTranslateNoExpr: Unit = {
    val translator = new QueryDefinitionTranslator(new ExpressionTranslator(mappings))

    val noExpr = QueryDefinition("foo", None)
    
    translator.translate(noExpr) should equal(noExpr)
    
    val noExprButSubqueries = QueryDefinition("foo", expr = None, subQueries = Seq(queryDef(Term("oneMatch"))))
    val expected = QueryDefinition("foo", expr = None, subQueries = Seq(queryDef(Term("localTerm3"))))
    
    translator.translate(noExprButSubqueries) should equal(expected)
  }

  @Test
  def testTranslateQueryDefWithOptionalFields: Unit = {
    val translator = new QueryDefinitionTranslator(new ExpressionTranslator(mappings))

    translator.translate(queryDefWithOptionalFields(Term("oneMatch"))) should equal(queryDefWithOptionalFields(Term("localTerm3")))
    translator.translate(queryDefWithOptionalFields(Term("twoMatches"))) should equal(queryDefWithOptionalFields(Or(Term("localTerm1"), Term("localTerm2"))))
  }

  @Test
  def testOnFailedMapping: Unit = {
    val unmappedTerm = Term("alskjklasdjl")
    val mappedTerm = Term("oneMatch") 
    
    val unmapped = queryDef(unmappedTerm)
    
    //expr is unmapped, subqueries' exprs aren't
    val unmappedWithOptionalFields0 = queryDefWithOptionalFields(unmappedTerm)
    //expr is unmapped, subqueries' exprs are and aren't
    val unmappedWithOptionalFields1 = queryDefWithOptionalFields(unmappedTerm).copy(subQueries = Seq(queryDef(mappedTerm), queryDef(unmappedTerm)))
    //expr is unmapped, subqueries' exprs are
    val unmappedWithOptionalFields2 = queryDefWithOptionalFields(unmappedTerm).copy(subQueries = Seq(queryDef(mappedTerm), queryDef(mappedTerm)))
    //expr is mapped, subqueries' exprs aren't
    val unmappedWithOptionalFields3 = queryDefWithOptionalFields(unmappedTerm).copy(expr = Some(mappedTerm))

    val translator = new QueryDefinitionTranslator(new ExpressionTranslator(mappings))

    intercept[MappingException] {
      translator.translate(unmapped)
    }
    
    intercept[MappingException] {
      translator.translate(unmappedWithOptionalFields0)
    }

    intercept[MappingException] {
      translator.translate(unmappedWithOptionalFields1)
    }
    
    intercept[MappingException] {
      translator.translate(unmappedWithOptionalFields2)
    }
    
    intercept[MappingException] {
      translator.translate(unmappedWithOptionalFields3)
    }
  }
}