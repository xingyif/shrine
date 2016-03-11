package net.shrine.adapter.service

import junit.framework.TestCase
import net.shrine.adapter.AdapterTestHelpers
import net.shrine.adapter.dao.squeryl.AbstractSquerylAdapterTest
import net.shrine.client.{HttpClient, HttpResponse, JerseyHttpClient}
import net.shrine.crypto.TrustParam.AcceptAllCerts
import net.shrine.protocol.{DefaultBreakdownResultOutputTypes, ErrorResponse, I2b2AdminReadQueryDefinitionRequest, I2b2AdminRequestHandler, QueryMaster, ReadI2b2AdminPreviousQueriesRequest, ReadPreviousQueriesResponse, ReadQueryDefinitionResponse}
import net.shrine.protocol.query.QueryDefinition
import net.shrine.util.{ShouldMatchersForJUnit, XmlUtil}
import org.junit.{After, Before}

import scala.xml.XML

/**
 * @author clint
 * @since Apr 24, 2013
 */
abstract class AbstractI2b2AdminResourceJaxrsTest extends TestCase with JerseyTestComponent[I2b2AdminRequestHandler] with AbstractSquerylAdapterTest with ShouldMatchersForJUnit with CanLoadTestData with AdapterTestHelpers {

  import scala.concurrent.duration._
  
  protected def adminClient = I2b2AdminClient(resourceUrl, JerseyHttpClient(AcceptAllCerts, 5.minutes))

  override def resourceClass(handler: I2b2AdminRequestHandler) = I2b2AdminResource(handler, DefaultBreakdownResultOutputTypes.toSet)
  
  override val basePath = "i2b2/admin/request"
    
  @Before
  override def setUp(): Unit = this.JerseyTest.setUp()

  @After
  override def tearDown(): Unit = this.JerseyTest.tearDown()
  
  protected object NeverAuthenticatesMockPmHttpClient extends HttpClient {
    override def post(input: String, url: String): HttpResponse = HttpResponse.ok(ErrorResponse("blarg").toI2b2String)
  }
  
  protected object AlwaysAuthenticatesMockPmHttpClient extends HttpClient {
    override def post(input: String, url: String): HttpResponse = {
      HttpResponse.ok(XmlUtil.stripWhitespace {
        <response>
          <message_body>
            <configure>
              <user>
                <full_name>Some user</full_name>
                <user_name>{ userId }</user_name>
                <domain>{ domain }</domain>
                <password>{ password }</password>
                <project id={ projectId }>
                  <role>MANAGER</role>
                </project>
              </user>
            </configure>
          </message_body>
        </response>
      }.toString)
    }
  }
  
  protected def doTestReadQueryDefinition(networkQueryId: Long, expectedQueryNameAndQueryDef: Option[(String, QueryDefinition)]) {
    val request = I2b2AdminReadQueryDefinitionRequest(projectId, waitTime, authn, networkQueryId)

    val resp = adminClient.readQueryDefinition(request)

    def stripNamespaces(s: String) = XmlUtil.stripNamespaces(XML.loadString(s))

    expectedQueryNameAndQueryDef match {
      case Some((expectedQueryName, expectedQueryDef)) => {
        val response @ ReadQueryDefinitionResponse(masterId, name, userId, createDate, queryDefinition) = resp
        
        masterId should be(networkQueryId)
        name should be(expectedQueryName)
        userId should be(authn.username)
        createDate should not be (null)
        //NB: I'm not sure why whacky namespaces were coming back from the resource;
        //this checks that the gist of the queryDef XML makes it back.
        //TODO: revisit this
        stripNamespaces(queryDefinition) should equal(stripNamespaces(expectedQueryDef.toI2b2String))
      } 
      case None => resp.isInstanceOf[ErrorResponse] should be(true)
    }
  }
  
  protected def doTestReadI2b2AdminPreviousQueries(request: ReadI2b2AdminPreviousQueriesRequest, expectedQueryMasters: Seq[QueryMaster]) {
    val ReadPreviousQueriesResponse(queryMasters) = adminClient.readI2b2AdminPreviousQueries(request)

    if(expectedQueryMasters.isEmpty) { queryMasters.isEmpty should be(true) }
    else {

      (queryMasters zip expectedQueryMasters).foreach { case (queryMaster, expected) =>
        queryMaster.createDate should not be(null)
        queryMaster.name should equal(expected.name)
        queryMaster.queryMasterId should equal(expected.queryMasterId)
        queryMaster.userId should equal(expected.userId)
        queryMaster.groupId should equal(expected.groupId)
        queryMaster.flagged should equal(expected.flagged)
      }
    }
  }
}