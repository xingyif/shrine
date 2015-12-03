package net.shrine.protocol.query

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Dec 17, 2012
 */
final class SimplePlanTest extends ShouldMatchersForJUnit {
  //NB: Tests for SimplePlan.normalize handled by ExpressionTest.test*ToExecutionPlan*()
  
  private val t1 = Term("1")
  private val t2 = Term("2")
  private val t3 = Term("3")
  
  @Test
  def testOr {
    val plan = SimplePlan(t1)
    
    plan.or(SimplePlan(t2)) should equal(SimplePlan(Or(t1, t2)))
    
    plan.or(CompoundPlan.Or(SimplePlan(t2), SimplePlan(t3))) should equal(CompoundPlan.Or(SimplePlan(t1), CompoundPlan.Or(SimplePlan(t2), SimplePlan(t3))))
    plan.or(CompoundPlan.And(SimplePlan(t2), SimplePlan(t3))) should equal(CompoundPlan.Or(SimplePlan(t1), CompoundPlan.And(SimplePlan(t2), SimplePlan(t3))))
  }
  
  @Test
  def testAnd {
    val plan = SimplePlan(t1)
    
    plan.and(SimplePlan(t2)) should equal(SimplePlan(And(t1, t2)))
    
    plan.and(CompoundPlan.Or(SimplePlan(t2), SimplePlan(t3))) should equal(CompoundPlan.And(SimplePlan(t1), CompoundPlan.Or(SimplePlan(t2), SimplePlan(t3))))
    plan.and(CompoundPlan.And(SimplePlan(t2), SimplePlan(t3))) should equal(CompoundPlan.And(SimplePlan(t1), CompoundPlan.And(SimplePlan(t2), SimplePlan(t3))))
  }

  //NB: Tests for SimplePlan.combine handled by testOr() and testAnd() 
  
  @Test
  def testWithExpr {
    val plan1 = SimplePlan(t1)
    
    (plan1 eq plan1.withExpr(t1)) should be(true)
    
    val plan2 = plan1.withExpr(t2)
    
    (plan1 eq plan2) should be(false)
    
    plan1.expr should equal(t1)
    plan2.expr should equal(t2)
  }
  
  @Test
  def testIsSimpleIsCompound {
    SimplePlan(t1).isSimple should be(true)
    SimplePlan(t1).isCompound should be(false)
  }
}