package net.shrine.jersey

import com.sun.jersey.api.core.DefaultResourceConfig
import net.shrine.wiring.ShrineJaxrsResources

/**
 * @author clint
 * @since Jan 14, 2014
 * 
 * Base class for Jersey "entry points".  When given a source of Shrine JAX-RS resource class instances, this
 * class registers them in such a way that Jersey can expose them.
 */
abstract class ShrineResourceConfig(jaxrsResources: ShrineJaxrsResources) extends DefaultResourceConfig {
  //NB: Passing an instance of a class annotated with @Path or other JAX-RS annotations to 
  //getSingletons().add() makes Jersey expose that instance as a JAX-RS resource.  This 
  //allows us to do our own wiring.
  jaxrsResources.resources.foreach(getSingletons.add)
}