package net.shrine.protocol

import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Or
import net.shrine.protocol.query.Term

trait AbstractTranslatedQueryDefinitionTest {
  protected val t1 = "foo"  
  protected val t2 = "bar"

  protected val expr = Or(Term(t1), Term(t2))
  
  protected val name = "blarg"
  
  protected val queryDef = QueryDefinition(name, expr)
}