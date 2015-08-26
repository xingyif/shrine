package net.shrine.utilities.audit

import org.junit.Test
import static junit.framework.Assert.assertEquals

/**
 * @author Bill Simons
 * @date 8/22/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
class AuditExportTest {

    @Test
    void testPrettyQueryDefinition() throws Exception {
        def queryDef = "<ns4:query_definition xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns8=\"http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/\" xmlns:ns2=\"http://www.i2b2.org/xsd/hive/pdo/1.1/\" xmlns:ns5=\"http://www.i2b2.org/xsd/hive/plugin/\" xmlns:ns3=\"http://www.i2b2.org/xsd/cell/crc/pdo/1.1/\" xmlns:ns7=\"http://www.i2b2.org/xsd/cell/ont/1.1/\" xmlns:ns4=\"http://www.i2b2.org/xsd/cell/crc/psm/1.1/\" xmlns:ns6=\"http://www.i2b2.org/xsd/hive/msg/1.1/\">\n" +
                "        <query_name>0 years old@18:29:23</query_name>\n" +
                "        <specificity_scale>0</specificity_scale>\n" +
                "        <use_shrine>1</use_shrine>\n" +
                "        <panel>\n" +
                "                <panel_number>1</panel_number>\n" +
                "                <invert>0</invert>\n" +
                "                <total_item_occurrences>1</total_item_occurrences>\n" +
                "                <item>\n" +
                "                        <hlevel>4</hlevel>\n" +
                "                        <item_name>0 years old</item_name>\n" +
                "                        <item_key>\\\\SHRINE\\SHRINE\\Demographics\\Age\\0-9 years old\\0 years old\\</item_key>\n" +
                "                        <tooltip>Demographics\\Age\\0-9 years old\\0 years old</tooltip>\n" +
                "                        <class>ENC</class>\n" +
                "                        <constrain_by_date>\n" +
                "                        </constrain_by_date>\n" +
                "                        <item_icon>LA</item_icon>\n" +
                "                        <item_is_synonym>false</item_is_synonym>\n" +
                "                </item>\n" +
                "                <item>\n" +
                "                        <hlevel>3</hlevel>\n" +
                "                        <item_name>Unknown</item_name>\n" +
                "                        <item_key>\\\\SHRINE\\SHRINE\\Demographics\\Gender\\Unknown\\</item_key>\n" +
                "                        <tooltip>Demographics\\Gender\\Unknown</tooltip>\n" +
                "                        <class>ENC</class>\n" +
                "                        <constrain_by_date>\n" +
                "                        </constrain_by_date>\n" +
                "                        <item_icon>LA</item_icon>\n" +
                "                        <item_is_synonym>false</item_is_synonym>\n" +
                "                </item>\n" +
                "        </panel>\n" +
                "        <panel>\n" +
                "                <panel_number>2</panel_number>\n" +
                "                <invert>1</invert>\n" +
                "                <total_item_occurrences>1</total_item_occurrences>\n" +
                "                <item>\n" +
                "                        <hlevel>4</hlevel>\n" +
                "                        <item_name>0 years old</item_name>\n" +
                "                        <item_key>\\\\SHRINE\\SHRINE\\Demographics\\Age\\0-9 years old\\0 years old\\</item_key>\n" +
                "                        <tooltip>Demographics\\Age\\0-9 years old\\0 years old</tooltip>\n" +
                "                        <class>ENC</class>\n" +
                "                        <constrain_by_date>\n" +
                "                        </constrain_by_date>\n" +
                "                        <item_icon>LA</item_icon>\n" +
                "                        <item_is_synonym>false</item_is_synonym>\n" +
                "                </item>\n" +
                "                <item>\n" +
                "                        <hlevel>3</hlevel>\n" +
                "                        <item_name>Unknown</item_name>\n" +
                "                        <item_key>\\\\SHRINE\\SHRINE\\Demographics\\Gender\\Unknown\\</item_key>\n" +
                "                        <tooltip>Demographics\\Gender\\Unknown</tooltip>\n" +
                "                        <class>ENC</class>\n" +
                "                        <constrain_by_date>\n" +
                "                        </constrain_by_date>\n" +
                "                        <item_icon>LA</item_icon>\n" +
                "                        <item_is_synonym>false</item_is_synonym>\n" +
                "                </item>\n" +
                "        </panel>\n" +
                "</ns4:query_definition>"

      def prettyDef = AuditExport.prettyQueryDefinition(queryDef)
      def expected = "(\\\\SHRINE\\SHRINE\\Demographics\\Age\\0-9 years old\\0 years old\\ OR \\\\SHRINE\\SHRINE\\Demographics\\Gender\\Unknown\\)\r\n AND NOT(\\\\SHRINE\\SHRINE\\Demographics\\Age\\0-9 years old\\0 years old\\ OR \\\\SHRINE\\SHRINE\\Demographics\\Gender\\Unknown\\)"
      assertEquals expected, prettyDef
    }
}