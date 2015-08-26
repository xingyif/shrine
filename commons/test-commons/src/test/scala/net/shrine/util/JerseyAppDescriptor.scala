package net.shrine.util

import com.sun.jersey.api.core.DefaultResourceConfig
import com.sun.jersey.api.core.ResourceConfig
import com.sun.jersey.test.framework.AppDescriptor
import com.sun.jersey.test.framework.LowLevelAppDescriptor.Builder

/**
 * @author clint
 * @date Apr 10, 2013
 */
final class JerseyAppDescriptor[H <: AnyRef](makeResource: H => AnyRef) {
  //Make an appdescriptor that exposes the @Resource class obtained by calling makeResource() with
  //the passed in parameter
  def using(handler: => H): AppDescriptor = {
    def appDescriptor(resourceConfig: ResourceConfig): AppDescriptor = new Builder(resourceConfig).build
    
    appDescriptor(resourceConfig(makeResource(handler)))
  }

  //Make a simple Jersey ResourceConfig that exposes the passed-in @Resource instance as a singleton
  def resourceConfig(resource: AnyRef): ResourceConfig = {
    val config = new DefaultResourceConfig
    
    config.getSingletons.add(resource)
    
    config
  } 
}

object JerseyAppDescriptor {
  def thatCreates[H <: AnyRef](f: H => AnyRef) = new JerseyAppDescriptor(f)
}