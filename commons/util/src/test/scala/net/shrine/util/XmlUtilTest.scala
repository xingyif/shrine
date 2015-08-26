package net.shrine.util

import org.junit.Test
import xml.XML

/**
 * @author Bill Simons
 * @date 2/14/12
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final class XmlUtilTest extends ShouldMatchersForJUnit {
  @Test
  def testCondense: Unit = {
    import XmlUtil.condense

    val alreadyCondensed = <foo>bar</foo>

    condense(alreadyCondensed) should equal(alreadyCondensed)
    condense(<foo> bar</foo>) should equal(alreadyCondensed)
    condense(<foo>bar  </foo>) should equal(alreadyCondensed)
    condense(<foo>    bar    </foo>) should equal(alreadyCondensed)

    condense(<foo>
               bar
             </foo>) should equal(alreadyCondensed)

    {
      val nested = {
        <baz>
          <blarg>
            <foo>
              bar
            </foo>
          </blarg>
        </baz>
      }

      val expected = {
        <baz>
          <blarg>
            <foo>bar</foo>
          </blarg>
        </baz>
      }

      condense(nested) should equal(expected)
    }

    {
      val complex = {
        <query_definition>
          <query_name>Acquired hemoly@13:24:42</query_name>
          <query_timing>ANY</query_timing>
          <specificity_scale>0</specificity_scale>
          <use_shrine>1</use_shrine>
          <panel>
            <panel_number>1</panel_number>
            <invert>0</invert>
            <panel_timing>ANY</panel_timing>
            <total_item_occurrences>1</total_item_occurrences>
            <item>
              <hlevel>3</hlevel>
              <item_name>
                \\SHRINE\SHRINE\Diagnoses\Diseases of the blood and blood-forming organs (280-289.99)\Acquired hemolytic anemias (283)\
              </item_name>
              <item_key>
                \\SHRINE\SHRINE\Diagnoses\Diseases of the blood and blood-forming organs (280-289.99)\Acquired hemolytic anemias (283)\
              </item_key>
              <tooltip>
                \\SHRINE\SHRINE\Diagnoses\Diseases of the blood and blood-forming organs (280-289.99)\Acquired hemolytic anemias (283)\
              </tooltip>
              <class>ENC</class>
              <constrain_by_date/>
              <item_icon>LA</item_icon>
              <item_is_synonym>false</item_is_synonym>
            </item>
          </panel>
        </query_definition>
      }

      val expected = {
        <query_definition>
          <query_name>Acquired hemoly@13:24:42</query_name>
          <query_timing>ANY</query_timing>
          <specificity_scale>0</specificity_scale>
          <use_shrine>1</use_shrine>
          <panel>
            <panel_number>1</panel_number>
            <invert>0</invert>
            <panel_timing>ANY</panel_timing>
            <total_item_occurrences>1</total_item_occurrences>
            <item>
              <hlevel>3</hlevel>
              <item_name>\\SHRINE\SHRINE\Diagnoses\Diseases of the blood and blood-forming organs (280-289.99)\Acquired hemolytic anemias (283)\</item_name>
              <item_key>\\SHRINE\SHRINE\Diagnoses\Diseases of the blood and blood-forming organs (280-289.99)\Acquired hemolytic anemias (283)\</item_key>
              <tooltip>\\SHRINE\SHRINE\Diagnoses\Diseases of the blood and blood-forming organs (280-289.99)\Acquired hemolytic anemias (283)\</tooltip>
              <class>ENC</class>
              <constrain_by_date/>
              <item_icon>LA</item_icon>
              <item_is_synonym>false</item_is_synonym>
            </item>
          </panel>
        </query_definition>
      }

      condense(complex) should equal(expected)
    }
  }

  @Test
  def testTrim: Unit = {
    import XmlUtil.trim

    trim(<foo>bar</foo>) should equal("bar")
    trim(<foo> bar</foo>) should equal("bar")
    trim(<foo>bar  </foo>) should equal("bar")
    trim(<foo>    bar    </foo>) should equal("bar")
  }

  @Test
  def testToInt: Unit = {
    intercept[Exception] {
      XmlUtil.toInt(<foo>bar</foo>)
    }

    intercept[Exception] {
      XmlUtil.toInt(<foo></foo>)
    }

    intercept[Exception] {
      XmlUtil.toInt(<foo>  </foo>)
    }

    intercept[Exception] {
      XmlUtil.toInt(<foo>123bar</foo>)
    }

    XmlUtil.toInt(<foo>123 </foo>) should equal(123)
    XmlUtil.toInt(<foo> 123</foo>) should equal(123)
    XmlUtil.toInt(<foo> 123 </foo>) should equal(123)
    XmlUtil.toInt(<foo>123</foo>) should equal(123)
    XmlUtil.toInt(<foo>0</foo>) should equal(0)
    XmlUtil.toInt(<foo>-123</foo>) should equal(-123)
  }

  @Test
  def testToLong: Unit = {
    intercept[Exception] {
      XmlUtil.toLong(<foo>bar</foo>)
    }

    intercept[Exception] {
      XmlUtil.toLong(<foo></foo>)
    }

    intercept[Exception] {
      XmlUtil.toLong(<foo>  </foo>)
    }

    intercept[Exception] {
      XmlUtil.toLong(<foo>123bar</foo>)
    }

    XmlUtil.toLong(<foo>123 </foo>) should equal(123L)
    XmlUtil.toLong(<foo> 123</foo>) should equal(123L)
    XmlUtil.toLong(<foo> 123 </foo>) should equal(123L)
    XmlUtil.toLong(<foo>123</foo>) should equal(123L)
    XmlUtil.toLong(<foo>0</foo>) should equal(0L)
    XmlUtil.toLong(<foo>-123</foo>) should equal(-123L)
  }

  @Test
  def testLoadStringIgnoringRemoteResources: Unit = {
    val xml = <foo><bar>  <baz/>  <nuh><zuh>123</zuh>     </nuh></bar></foo>

    val loaded = XmlUtil.loadStringIgnoringRemoteResources(xml.toString)

    loaded should equal(Some(xml))

    val loadedViaXmlLoadString = XML.loadString(xml.toString)

    loadedViaXmlLoadString should equal(xml)

    loaded.get should equal(loadedViaXmlLoadString)

    //TODO: Test with nefarious DTD URL
  }

  @Test
  def testStripWhitespace: Unit = {
    val node = XML.loadString("<foo>\n\t<bar>  baz     </bar>\n</foo>")

    XmlUtil.stripWhitespace(node).toString() should equal("<foo><bar>  baz     </bar></foo>")
  }

  @Test
  def testRenameRootTag: Unit = {
    val xml = <foo><bar><baz/></bar></foo>

    val expected = <blarg><bar><baz/></bar></blarg>

    XmlUtil.renameRootTag("blarg")(xml).toString should equal(expected.toString)
  }

  @Test
  def testPrettyPrint: Unit = {
    {
      val xml = <foo><bar><baz/></bar><blerg>123</blerg></foo>

      val expected = {
        """<foo>
  <bar>
    <baz/>
  </bar>
  <blerg>123</blerg>
</foo>"""
      }

      XmlUtil.prettyPrint(xml) should equal(expected)
    }

    {
      val complex = {
        <query_definition>
          <query_name>Acquired hemoly@13:24:42</query_name>
          <query_timing>ANY</query_timing>
          <specificity_scale>0</specificity_scale>
          <use_shrine>1</use_shrine>
          <panel>
            <panel_number>1</panel_number>
            <invert>0</invert>
            <panel_timing>ANY</panel_timing>
            <total_item_occurrences>1</total_item_occurrences>
            <item>
              <hlevel>3</hlevel>
              <item_name>
                \\SHRINE\SHRINE\Diagnoses\Diseases of the blood and blood-forming organs (280-289.99)\Acquired hemolytic anemias (283)\
              </item_name>
              <item_key>
                \\SHRINE\SHRINE\Diagnoses\Diseases of the blood and blood-forming organs (280-289.99)\Acquired hemolytic anemias (283)\
              </item_key>
              <tooltip>
                \\SHRINE\SHRINE\Diagnoses\Diseases of the blood and blood-forming organs (280-289.99)\Acquired hemolytic anemias (283)\
              </tooltip>
              <class>ENC</class>
              <constrain_by_date/>
              <item_icon>LA</item_icon>
              <item_is_synonym>false</item_is_synonym>
            </item>
          </panel>
        </query_definition>
      }

      val expected = {
        """<query_definition>
  <query_name>Acquired hemoly@13:24:42</query_name>
  <query_timing>ANY</query_timing>
  <specificity_scale>0</specificity_scale>
  <use_shrine>1</use_shrine>
  <panel>
    <panel_number>1</panel_number>
    <invert>0</invert>
    <panel_timing>ANY</panel_timing>
    <total_item_occurrences>1</total_item_occurrences>
    <item>
      <hlevel>3</hlevel>
      <item_name>\\SHRINE\SHRINE\Diagnoses\Diseases of the blood and blood-forming organs (280-289.99)\Acquired hemolytic anemias (283)\</item_name>
      <item_key>\\SHRINE\SHRINE\Diagnoses\Diseases of the blood and blood-forming organs (280-289.99)\Acquired hemolytic anemias (283)\</item_key>
      <tooltip>\\SHRINE\SHRINE\Diagnoses\Diseases of the blood and blood-forming organs (280-289.99)\Acquired hemolytic anemias (283)\</tooltip>
      <class>ENC</class>
      <constrain_by_date/>
      <item_icon>LA</item_icon>
      <item_is_synonym>false</item_is_synonym>
    </item>
  </panel>
</query_definition>"""
      }

      XmlUtil.prettyPrint(complex) should equal(expected)
    }
  }

  @Test
  def testSurroundWith: Unit = {
    import XmlUtil._

    stripWhitespace(surroundWith(<foo/>)(<bar/><baz/>).head) should equal(<foo><bar/><baz/></foo>)

    stripWhitespace(surroundWith("foo")(<bar/><baz/>).head) should equal(<foo><bar/><baz/></foo>)
  }
}