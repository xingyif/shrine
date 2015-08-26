package net.shrine.ont.messaging

import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.serialization.JsonMarshaller

/**
 * @author Clint Gilbert
 * @date Feb 8, 2012
 */
abstract class FromJsonTest[T <: JsonMarshaller](unmarshaller: LiftJsonUnmarshaller[T]) extends ShouldMatchersForJUnit {
  protected def doTestFromJson(thing: T) {
    val json = thing.toJsonString()
    
    val unmarshalled = unmarshaller.fromJson(json)
    
    unmarshalled should equal(thing)
  }
}