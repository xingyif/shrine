package net.shrine.protocol.query

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Dec 17, 2012
 */
final class CompoundPlanTest extends ShouldMatchersForJUnit {
  private val t1 = Term("1")
  private val t2 = Term("2")
  private val t3 = Term("3")
  private val t4 = Term("4")
  
  private val q1 = Query("12345")
  
  @Test
  def testToString {
    val plans = Seq(SimplePlan(t1), SimplePlan(t2))
    
    CompoundPlan.Or(plans: _*).toString should equal("CompoundPlan.Or(" + plans.mkString(",") + ")")
    CompoundPlan.And(plans: _*).toString should equal("CompoundPlan.And(" + plans.mkString(",") + ")")
  }
  
  @Test
  def testCombineAndOr {
    //NB: Also exercises CompoundPlan.{and, or}
    
    CompoundPlan.Or(SimplePlan(t1)).combine(Conjunction.Or)(SimplePlan(t2)) should equal(CompoundPlan.Or(CompoundPlan.Or(SimplePlan(t1)), SimplePlan(t2)))
    
    CompoundPlan.And(SimplePlan(t1)).combine(Conjunction.Or)(SimplePlan(t2)) should equal(CompoundPlan.Or(CompoundPlan.And(SimplePlan(t1)), SimplePlan(t2)))
    
    CompoundPlan.Or(SimplePlan(t1)).combine(Conjunction.And)(SimplePlan(t2)) should equal(CompoundPlan.And(CompoundPlan.Or(SimplePlan(t1)), SimplePlan(t2)))
    
    CompoundPlan.And(SimplePlan(t1)).combine(Conjunction.And)(SimplePlan(t2)) should equal(CompoundPlan.And(CompoundPlan.And(SimplePlan(t1)), SimplePlan(t2)))
  }
  
  @Test
  def testIsSimpleIsCompound {
    CompoundPlan.Or().isSimple should be(false)
    CompoundPlan.Or().isCompound should be(true)
    
    CompoundPlan.And().isSimple should be(false)
    CompoundPlan.And().isCompound should be(true)
  }
  
  @Test
  def testIsSame {
    CompoundPlan.Or().isSame(Conjunction.And) should be(false)
    CompoundPlan.Or().isSame(Conjunction.Or) should be(true)
    
    CompoundPlan.And().isSame(Conjunction.And) should be(true)
    CompoundPlan.And().isSame(Conjunction.Or) should be(false)
  }

  //NB: ------- Companion object methods follow --------
  
  @Test
  def testOr {
    for(exprs <- Seq[Seq[ExecutionPlan]](Nil, Seq(SimplePlan(t1)), Seq(SimplePlan(t1), SimplePlan(t2)))) {
      val plan = CompoundPlan.Or(exprs: _*)
      
      plan.conjunction should equal(Conjunction.Or)
      
      plan.components should equal(exprs)
    }
  }
  
  @Test
  def testAnd {
    for(exprs <- Seq[Seq[ExecutionPlan]](Nil, Seq(SimplePlan(t1)), Seq(SimplePlan(t1), SimplePlan(t2)))) {
      val plan = CompoundPlan.And(exprs: _*)
      
      plan.conjunction should equal(Conjunction.And)
      
      plan.components should equal(exprs)
    }
  }
  
  @Test
  def testAllSimple {
    import CompoundPlan.Helpers.allSimple
    
    allSimple(Nil) should be(true)
    
    allSimple(Seq(SimplePlan(t1))) should be(true)
    allSimple(Seq(SimplePlan(t1), SimplePlan(t2))) should be(true)
    
    allSimple(Seq(CompoundPlan.Or(SimplePlan(t1)))) should be(false)
    allSimple(Seq(CompoundPlan.And(SimplePlan(t1)))) should be(false)
    
    allSimple(Seq(SimplePlan(t1), CompoundPlan.Or(SimplePlan(t2)))) should be(false)
    allSimple(Seq(SimplePlan(t1), CompoundPlan.And(SimplePlan(t2)))) should be(false)
    
    allSimple(Seq(CompoundPlan.Or(SimplePlan(t1)), SimplePlan(t2))) should be(false)
    allSimple(Seq(CompoundPlan.And(SimplePlan(t1)), SimplePlan(t2))) should be(false)
  }
  
  @Test
  def testAnds {
    import CompoundPlan.Helpers.ands
    
    ands(Nil) should equal(Nil)
    ands(Seq(SimplePlan(Or(t1, t2)))) should equal(Nil)
    ands(Seq(SimplePlan(And(t1, t2)))) should equal(Seq(And(t1, t2)))
    ands(Seq(SimplePlan(And(t1, t2)), SimplePlan(And(q1, t3)))) should equal(Seq(And(t1, t2), And(q1, t3)))
    ands(Seq(SimplePlan(Or(t1, t2)), SimplePlan(And(q1, t3)), SimplePlan(t4))) should equal(Seq(And(q1, t3)))
  }
  
  @Test
  def testOrs {
    import CompoundPlan.Helpers.ors
    
    ors(Nil) should equal(Nil)
    ors(Seq(SimplePlan(And(t1, t2)))) should equal(Nil)
    ors(Seq(SimplePlan(Or(t1, t2)))) should equal(Seq(Or(t1, t2)))
    ors(Seq(SimplePlan(Or(t1, t2)), SimplePlan(Or(q1, t3)))) should equal(Seq(Or(t1, t2), Or(q1, t3)))
    ors(Seq(SimplePlan(And(t1, t2)), SimplePlan(Or(q1, t3)), SimplePlan(t4))) should equal(Seq(Or(q1, t3)))
  }
  
  @Test
  def testNeitherAndsNorOrs {
    import CompoundPlan.Helpers.neitherAndsNorOrs
    
    neitherAndsNorOrs(Nil) should equal(Nil)
    neitherAndsNorOrs(Seq(SimplePlan(And(t1, t2)))) should equal(Nil)
    neitherAndsNorOrs(Seq(SimplePlan(Or(t1, t2)))) should equal(Nil)
    neitherAndsNorOrs(Seq(SimplePlan(Or(t1, t2)), SimplePlan(Or(q1, t3)))) should equal(Nil)
    neitherAndsNorOrs(Seq(SimplePlan(Or(t1, t2)), SimplePlan(Or(q1, t3)), SimplePlan(t4))) should equal(Seq(SimplePlan(t4)))
  }
  
  @Test
  def testAndHasZero {
    CompoundPlan.Helpers.HasZero.andHasZero.zero.isInstanceOf[And] should be(true)
    CompoundPlan.Helpers.HasZero.andHasZero.zero.exprs.isEmpty should be(true)
  }
  
  @Test
  def testOrHasZero {
    CompoundPlan.Helpers.HasZero.orHasZero.zero.isInstanceOf[Or] should be(true)
    CompoundPlan.Helpers.HasZero.orHasZero.zero.exprs.isEmpty should be(true)
  }
  
  @Test
  def testFlatten {
    import CompoundPlan.Helpers.flatten
    
    flatten[And](Nil) should equal(And())
    flatten(Seq(And(), And())) should equal(And())
    flatten(Seq(And(), And(t1, q1))) should equal(And(t1, q1))
    flatten(Seq(And(t1, q1), And())) should equal(And(t1, q1))
    flatten(Seq(And(t1), And(q1))) should equal(And(t1, q1))
    flatten(Seq(And(t1), And(q1), And(t2, t3))) should equal(And(t1, q1, t2, t3))
    
    flatten[Or](Nil) should equal(Or())
    flatten(Seq(Or(), Or())) should equal(Or())
    flatten(Seq(Or(), Or(t1, q1))) should equal(Or(t1, q1))
    flatten(Seq(Or(t1, q1), Or())) should equal(Or(t1, q1))
    flatten(Seq(Or(t1), Or(q1))) should equal(Or(t1, q1))
    flatten(Seq(Or(t1), Or(q1), Or(t2, t3))) should equal(Or(t1, q1, t2, t3))
  }
  
  @Test
  def testToPlans {
    import CompoundPlan.Helpers.toPlans
    
    toPlans(Nil) should equal(Nil)
    toPlans(Seq(t1, q1)) should equal(Seq(SimplePlan(t1), SimplePlan(q1)))
    
    val Seq(CompoundPlan(actualConj, components @ _*)) = toPlans(Seq(Or(t1, And(q1, t2)))) 
    
    actualConj should equal(Conjunction.Or)
    components.toSet should equal(Set(SimplePlan(t1), SimplePlan(And(q1, t2))))
  }
}