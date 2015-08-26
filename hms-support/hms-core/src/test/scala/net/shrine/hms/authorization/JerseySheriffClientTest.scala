package net.shrine.hms.authorization

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.ApprovedTopic
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter

/**
 * @author clint
 * @date Apr 3, 2014
 */
final class JerseySheriffClientTest extends ShouldMatchersForJUnit {
  @Test
  def testEscapeQueryText: Unit = {
    val queryText = """<query_definition><query_name>Acute posthemor@15:54:08</query_name><specificity_scale>0</specificity_scale><use_shrine>1</use_shrine><panel><panel_number>1</panel_number><invert>0</invert><total_item_occurrences>1</total_item_occurrences><item><hlevel>4</hlevel><item_name>Acute posthemorrhagic anemia</item_name><item_key>\\SHRINE\SHRINE\Diagnoses\Diseases of the blood and blood-forming organs\Anemia\Acute posthemorrhagic anemia\</item_key><tooltip>Diagnoses\Diseases of the blood and blood-forming organs\Anemia\Acute posthemorrhagic anemia</tooltip><class>ENC</class><constrain_by_date></constrain_by_date><item_icon>FA</item_icon><item_is_synonym>false</item_is_synonym></item></panel></query_definition>"""

    val actual = JerseySheriffClient.escapeQueryText(queryText)

    //NB: As of Scala 2.10, XmlUtility.stripWhitespace now collapses elements like
    //<foo></foo> to <foo/> :\
    actual should equal("""&lt;query_definition&gt;&lt;query_name&gt;Acute posthemor@15:54:08&lt;/query_name&gt;&lt;specificity_scale&gt;0&lt;/specificity_scale&gt;&lt;use_shrine&gt;1&lt;/use_shrine&gt;&lt;panel&gt;&lt;panel_number&gt;1&lt;/panel_number&gt;&lt;invert&gt;0&lt;/invert&gt;&lt;total_item_occurrences&gt;1&lt;/total_item_occurrences&gt;&lt;item&gt;&lt;hlevel&gt;4&lt;/hlevel&gt;&lt;item_name&gt;Acute posthemorrhagic anemia&lt;/item_name&gt;&lt;item_key&gt;\\\\SHRINE\\SHRINE\\Diagnoses\\Diseases of the blood and blood-forming organs\\Anemia\\Acute posthemorrhagic anemia\\&lt;/item_key&gt;&lt;tooltip&gt;Diagnoses\\Diseases of the blood and blood-forming organs\\Anemia\\Acute posthemorrhagic anemia&lt;/tooltip&gt;&lt;class&gt;ENC&lt;/class&gt;&lt;constrain_by_date/&gt;&lt;item_icon&gt;FA&lt;/item_icon&gt;&lt;item_is_synonym&gt;false&lt;/item_is_synonym&gt;&lt;/item&gt;&lt;/panel&gt;&lt;/query_definition&gt;""")
  }

  @Test
  def testApprovedParseTopics: Unit = {
    val parsedTopics = JerseySheriffClient.parseApprovedTopics("""[{"id":1,"name":"q1"},{"id":2,"name":"query0"},{"id":3,"name":"query1"},{"id":4,"name":"query2"}]""")
    
    parsedTopics should not be (null)
    parsedTopics.size should equal(4)
    parsedTopics should contain(ApprovedTopic(1, "q1"))
  }

  @Test
  def testParseAuthorizationResponse: Unit = {
    
    JerseySheriffClient.parseAuthorizationResponse("""{"approved":true}""") should equal(true)
    JerseySheriffClient.parseAuthorizationResponse("""{"approved":false}""") should equal(false)
  }
  
  @Test
  def testCredentialsAreApplied: Unit = {
    val url = "http://example.com"
    val username = "u"
    val password = "p"
    
    val client = JerseySheriffClient(url, username, password)
    
    client.resource should not be(null)
    client.resource.toString should equal(url)

    //NB: It would be nice to know if we added the right credentials to the Jersey resource,
    //but HTTPBasicAuthFilter perhaps prudently does not store the supplied credentials directly,
    //but in an encoded form.  Since there's no way to test that the right credentials are there
    //in the auth filter present on the Jersey resource without duplicating the credential-encoding
    //logic, we settle for just testing that an HTTPBasicAuthFilter is present.
    client.resource.getHeadHandler.getClass should equal(classOf[HTTPBasicAuthFilter])
  }
}