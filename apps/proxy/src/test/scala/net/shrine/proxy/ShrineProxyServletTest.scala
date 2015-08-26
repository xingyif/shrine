package net.shrine.proxy

import java.io.BufferedReader
import java.io.PrintWriter
import java.io.StringReader
import java.io.StringWriter
import java.util
import java.util.Locale
import javax.servlet.{ServletContext, DispatcherType, ServletResponse, ServletRequest, AsyncContext, ServletException}
import scala.xml.NodeSeq
import scala.xml.XML
import javax.servlet.http.{Part, Cookie, HttpServletRequest, HttpServletResponse}
import net.shrine.client.HttpResponse
import net.shrine.util.XmlUtil
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author clint
 * @since Jun 25, 2012
 */
final class ShrineProxyServletTest extends ShouldMatchersForJUnit {
  def testDefaultConstructor {
    val servlet = new ShrineProxyServlet
    
    servlet.proxy.isInstanceOf[DefaultShrineProxy] should be(true)
    
    val proxy = servlet.proxy.asInstanceOf[DefaultShrineProxy]
    
    proxy.httpClient should be(DefaultShrineProxy.jerseyHttpClient)
    //whitelist values from shrine-proxy-acl.xml on this module's classpath
    proxy.whiteList should be(Set("http://127.0.0.1:7070/axis2/rest/", "http://localhost:7070/axis2/rest/", "http://webservices.i2b2.org/", "https://", "http://"))
    
    intercept[IllegalArgumentException] {
      new ShrineProxyServlet(null)
    }
  }
  
  private final class AlwaysFailsMockShrineProxy extends ShrineProxy {
    override def isAllowableUrl(redirectURL: String): Boolean = false
  
    override def redirect(request: NodeSeq): HttpResponse = {
      throw new Exception("foo") with scala.util.control.NoStackTrace
    }
  }
  
  def testDoPost {
    val whiteList = Set("http://example.com")
    
    val blackList = Set("http://malware.com") 
    
    //Should work 
    
    val shouldWork = Seq("http://example.com", "http://example.com/foo", "http://example.com/lots/of/stuff")
    
    val statuses = Seq(200, 400, 500)
    
    for {
      url <- shouldWork
      statusCode <- statuses
    } {
      val mockUrlPoster = new DefaultShrineProxyTest.MockHttpClient(statusCode)
    
      val servlet = new ShrineProxyServlet(new DefaultShrineProxy(whiteList, blackList, mockUrlPoster))
      
      val mockRequest = new MockHttpServletRequest(url)
    
      val mockResponse = new MockHttpServletResponse
    
      servlet.doPost(mockRequest, mockResponse)
    
      mockResponse.buffer.toString should equal("OK")
      mockResponse.statusCode should equal(statusCode)
      
      mockUrlPoster.url should equal(url)

      //NB: This hoop-jumping is necessary because xml-marshalling round-trips can produce
      //xml with semantically identical, but literally different, namespace declaration orders. :( 
      
      val actual = XML.loadString(mockUrlPoster.input).child.map(XmlUtil.stripNamespaces).toSet.toString

      val expected = XML.loadString(mockRequest.reqXml).child.map(XmlUtil.stripNamespaces).toSet.toString
      
      actual should equal(expected)
    }
    
    //Should fail
    
    val shouldFail = Seq("http://google.com", null, "", "  ")
    
    for {
      url <- shouldFail
      statusCode <- statuses
    } {
      val mockUrlPoster = new DefaultShrineProxyTest.MockHttpClient(statusCode)
    
      val servlet = new ShrineProxyServlet(new DefaultShrineProxy(whiteList, blackList, mockUrlPoster))
      
      val mockRequest = new MockHttpServletRequest(url)
    
      val mockResponse = new MockHttpServletResponse
    
      intercept[ServletException] {
        servlet.doPost(mockRequest, mockResponse)
      }
      
      //Should we set the status code on the response to anything here?  Or will it be set automatically since doPost() throws?
    }
    
    intercept[ServletException] {
      val mockRequest = new MockHttpServletRequest("http://example.com")
    
      val mockResponse = new MockHttpServletResponse
      
      new ShrineProxyServlet(new AlwaysFailsMockShrineProxy).doPost(mockRequest, mockResponse)
    }
  }
  
  //Ugh :(
  private final class MockHttpServletRequest(redirectUrl: String) extends HttpServletRequest {
    val reqXml = DefaultShrineProxyTest.getQuery(redirectUrl).toString
    
    def getAuthType = ""

    def getCookies = null

    def getDateHeader(name: String) = 0L

    def getHeader(name: String) = ""

    def getHeaders(name: String) = null

    def getHeaderNames() = null

    def getIntHeader(name: String) = 0

    def getMethod() = ""

    def getPathInfo() = ""

    def getPathTranslated() = ""

    def getContextPath() = ""

    def getQueryString() = ""

    def getRemoteUser() = ""

    def isUserInRole(role: String) = true

    def getUserPrincipal() = null

    def getRequestedSessionId() = ""

    def getRequestURI() = ""

    def getRequestURL() = null

    def getServletPath() = ""

    def getSession(create: Boolean) = null

    def getSession() = null

    def isRequestedSessionIdValid() = true

    def isRequestedSessionIdFromCookie() = true

    def isRequestedSessionIdFromURL() = false

    def isRequestedSessionIdFromUrl() = false
    
    def getAttribute(name: String) = null

    def getAttributeNames() = null

    def getCharacterEncoding() = null

    def setCharacterEncoding(env: String) = ()

    def getContentLength() = reqXml.size

    def getContentType() = ""

    def getInputStream() = null

    def getParameter(name: String) = ""

    def getParameterNames() = null

    def getParameterValues(name: String) = null

    def getParameterMap() = null

    def getProtocol() = ""

    def getScheme() = ""

    def getServerName() = ""

    def getServerPort() = 80

    def getReader() = new BufferedReader(new StringReader(reqXml))

    def getRemoteAddr() = ""

    def getRemoteHost() = ""

    def setAttribute(name: String, o: AnyRef) = ()

    def removeAttribute(name: String) = ()

    def getLocale() = null

    def getLocales() = null

    def isSecure() = false

    def getRequestDispatcher(path: String) = null

    def getRealPath(path: String) = ""

    def getRemotePort() = 12345

    def getLocalName() = ""

    def getLocalAddr() = ""

    def getLocalPort() = 12345

    override def getParts: util.Collection[Part] = ???

    override def getPart(name: String): Part = ???

    override def authenticate(response: HttpServletResponse): Boolean = ???

    override def logout(): Unit = ???

    override def login(username: String, password: String): Unit = ???

    override def isAsyncStarted: Boolean = ???

    override def startAsync(): AsyncContext = ???

    override def startAsync(servletRequest: ServletRequest, servletResponse: ServletResponse): AsyncContext = ???

    override def getAsyncContext: AsyncContext = ???

    override def getDispatcherType: DispatcherType = ???

    override def isAsyncSupported: Boolean = ???

    override def getServletContext: ServletContext = ???
  }
  
  //Ugh :(
  private final class MockHttpServletResponse extends HttpServletResponse {
    val buffer = new StringWriter
    
    val writer = new PrintWriter(buffer)
    
    var statusCode: Int = _
    
    def addCookie(cookie: Cookie) { }

    def containsHeader(name: String) = false

    def encodeURL(url: String) = ""

    def encodeRedirectURL(url: String) = ""

    def encodeUrl(url: String) = ""

    def encodeRedirectUrl(url: String) = ""

    def sendError(sc: Int, msg: String) = ()

    def sendError(sc: Int) = ()

    def sendRedirect(location: String) = ()

    def setDateHeader(name: String, date: Long) = ()

    def addDateHeader(name: String, date: Long) = ()

    def setHeader(name: String, value: String) = ()

    def addHeader(name: String, value: String) = ()

    def setIntHeader(name: String, value: Int) = ()

    def addIntHeader(name: String, value: Int) = ()

    def setStatus(sc: Int) { statusCode = sc }

    def setStatus(sc: Int, sm: String) = ()
    
    def getCharacterEncoding() = ""

    def getContentType() = ""

    def getOutputStream() = null

    def getWriter() = writer

    def setCharacterEncoding(charset: String) = ()

    def setContentLength(len: Int) = ()

    def setContentType(t: String) = ()

    def setBufferSize(size: Int) = ()

    def getBufferSize() = 0

    def flushBuffer() = ()

    def resetBuffer() = ()

    def isCommitted() = true

    def reset() = ()

    def setLocale(loc: Locale) = ()

    def getLocale() = null

    override def getHeaderNames: util.Collection[String] = ???

    override def getStatus: Int = ???

    override def getHeaders(name: String): util.Collection[String] = ???

    override def getHeader(name: String): String = ???
  }
}