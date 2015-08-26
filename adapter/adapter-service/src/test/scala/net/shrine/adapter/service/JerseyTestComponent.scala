package net.shrine.adapter.service

import com.sun.jersey.test.framework.AppDescriptor
import com.sun.jersey.test.framework.JerseyTest
import javax.ws.rs.Path
import javax.ws.rs.Produces
import net.shrine.adapter.dao.squeryl.AbstractSquerylAdapterTest
import net.shrine.util.JerseyAppDescriptor
import net.shrine.util.AbstractPortSearchingJerseyTest
import com.sun.jersey.api.client.WebResource

/**
 * @author clint
 * @date Apr 12, 2013
 */
trait JerseyTestComponent[H <: AnyRef] {
  def basePath: String
  
  def makeHandler: H
  
  def resourceClass(handler: H): AnyRef
  
  def port: Int = 9999 
  
  trait MixableJerseyTest extends AbstractPortSearchingJerseyTest {
    lazy val handler: H = makeHandler
  
    private def uri = resource.getURI
    
    def resourceUrl = s"$uri$basePath"
    
    override protected def getPort(defaultPort: Int): Int = super.getPort(port)
    
    def actualPort: Int = uri.getPort
  }
  
  object JerseyTest extends MixableJerseyTest {
    override def configure: AppDescriptor = {
      JerseyAppDescriptor.thatCreates(resourceClass).using(handler)
    }
  }
  
  def handler: H = JerseyTest.handler
  
  def resource: WebResource = JerseyTest.resource
  
  def resourceUrl: String = JerseyTest.resourceUrl
  
  def actualPort: Int = JerseyTest.actualPort
}