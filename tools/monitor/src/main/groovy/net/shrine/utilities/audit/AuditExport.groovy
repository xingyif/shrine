package net.shrine.utilities.audit

import au.com.bytecode.opencsv.CSVWriter
import groovy.sql.Sql

/**
 * @author Justin Quan
 * @date May 8, 2011
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
class AuditExport {
  def config
  Sql sql

  AuditExport(config, Sql sql) {
    this.config = config
    this.sql = sql
  }

  def dumpSqlToCSV() {
    File f = new File(config.csvFile.toString())
    CSVWriter writer = new CSVWriter(new FileWriter(f))

    sql.eachRow("SELECT * from AUDIT_ENTRY") {row ->
      def qString = queryTextString(row.query_text)
      writer.writeNext([row.project, row.username, row.domain_name, row.time, row.query_topic, prettyQueryDefinition(qString)] as String[])
    }

    writer.flush()
    writer.close()
  }

  def queryTextString(queryText) {
    if(queryText instanceof java.sql.Clob) {
      Reader stream = queryText.getCharacterStream()
      StringWriter writer = new StringWriter()
      int ch = stream.read()
      while(ch != -1) {
        writer.write(ch)
        ch = stream.read()
      }
      writer.toString()
    } else {
      queryText
    }
  }

  static def prettyQueryDefinition(String queryText) {
    def query = new XmlSlurper().parseText(queryText)
    query.panel.collect { panel ->
      def builder = new StringBuilder()
      if(panel.invert == 1) {
        builder += "NOT"
      }
      builder += "("
      builder += panel.item.item_key.collect {it}.join(" OR ")
      builder += ")"
      builder.toString()
    }.join("\r\n AND ")
  }

  public static void main(String[] args) {
    def config = new ConfigSlurper().parse(new File(args[0]).toURL())
    def sql = Sql.newInstance(config.dbUrl, config.dbUser, config.dbPasswd ?: "", config.dbDriver)
    def audit = new AuditExport(config, sql);
    audit.dumpSqlToCSV()
  }
}
