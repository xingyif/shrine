package net.shrine.adapter

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.adapter.translators.QueryDefinitionTranslator
import net.shrine.adapter.translators.ExpressionTranslator
import net.shrine.protocol.NodeId
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Or
import net.shrine.protocol.query.Term
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.ReadTranslatedQueryDefinitionRequest
import net.shrine.protocol.SingleNodeReadTranslatedQueryDefinitionResponse

/**
 * @author clint
 * @date Feb 14, 2014
 */
final class ReadTranslatedQueryDefinitionAdapterTest extends ShouldMatchersForJUnit {
  @Test
  def testProcessRequest {
    val nodeId = NodeId("nuh")
    
    val nt1 = "foo"
    val nt2 = "bar"
    val nt3 = "baz"
    
    val lt1 = "localFoo"
    val lt2 = "localBar"
    val lt3 = "localBaz"
      
    val mappings = Map(
        nt1 -> Set(lt1),
        nt2 -> Set(lt2),
        nt3 -> Set(lt3))
    
    val translator = new QueryDefinitionTranslator(new ExpressionTranslator(mappings))
    
    val adapter = new ReadTranslatedQueryDefinitionAdapter(nodeId, translator)
    
    val name = "asdf"
    
    val queryDef = QueryDefinition(name, Or(Term(nt1), Term(nt2), Term(nt3)))
    
    val authn = AuthenticationInfo("d", "u", Credential("p", false))
    
    import scala.concurrent.duration._
    
    val waitTime = 5.seconds
    
    val message = BroadcastMessage(authn, ReadTranslatedQueryDefinitionRequest(authn, waitTime, queryDef))
    
    val resp = adapter.processRequest(message).asInstanceOf[SingleNodeReadTranslatedQueryDefinitionResponse]
    
    resp.translated should equal(Seq(resp.result))
    
    resp.result.nodeId should equal(nodeId)
    
    resp.result.queryDef.name should equal(name)
    resp.result.queryDef.expr.get.isInstanceOf[Or] should be(true)
    
    val or = resp.result.queryDef.expr.get.asInstanceOf[Or]
    
    or.exprs.toSet should equal(Set(Term(lt1), Term(lt2), Term(lt3)))
  }
}