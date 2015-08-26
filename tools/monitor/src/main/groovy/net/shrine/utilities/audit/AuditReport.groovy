package net.shrine.utilities.audit

import groovy.sql.Sql
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * @author Justin Quan
 * @date 9/27/11
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 *
 * This script queries the shrine mysql audit db and creates a report over
 * a configurable number of days and is emailed out to interested parties.
 */
class AuditReport {
  def config
  Sql sql

  AuditReport(config, Sql sql) {
    this.config = config
    this.sql = sql
  }

  // ehhhhhh, mysql support only...
  def collectData() {
    def summary = [:] // names -> projects -> [queries]
    sql.eachRow('SELECT * from AUDIT_ENTRY WHERE DATE_SUB(CURDATE(),INTERVAL ? DAY) <= TIME',[config.numDays]) {row ->
      if(!summary.containsKey(row.username))
        summary[row.username] = [:]
      if(!summary[row.username].containsKey(row.project))
        summary[row.username][row.project] = []
      summary[row.username][row.project].add(prettyQueryDefinition(queryTextString(row.query_text)))
    }
    summary
  }

  def genTopUsersReport(data) {
    def msg = new StringBuilder()
    msg +=("--- Top Users by Query Count ---\n")
    data.keySet().sort{-1*data[it].values().flatten().size()}.each { user ->
      msg += "${user} ${data[user].values().flatten().size()}\n"
    }
    msg += "\n\n"
    msg.toString()
  }

  def genQueryReport(data) {
    def msg = new StringBuilder()
    msg +=("--- Query Frequency ---\n")
    data.keySet().sort{-1*data[it].values().flatten().size()}.each { user ->
      msg += "${user}\n"
      data[user].each { projName, queries ->
        msg += "  ${projName}\n"
        def uniq = queries.clone()
        uniq.unique().sort{-1*queries.count(it)}.each {query ->
          if(query) {
            msg += "    ${queries.count(query)}x${query}\n"
          }
        }
      }
    }

    msg += "\n\n"
    msg.toString()
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

  def email(String message) {
    Properties properties = new Properties();
    properties.setProperty("mail.smtp.host",config.emailHost);

    Session session = Session.getDefaultInstance(properties,null);

    def to = config.emailReceipients.split().collect {new InternetAddress(it)}.toArray().asType(InternetAddress[].class)
    Message mail = new MimeMessage(session);
    mail.setFrom(new InternetAddress(config.emailSender));
    mail.setRecipients(MimeMessage.RecipientType.TO,to);
    mail.setSubject(config.emailSubject);
    mail.setText(message)

    Transport.send(mail)
  }

  /**
   * You'll need your config properties file to have the following keys
   * dbUrl
   * dbUser
   * dbPasswd
   * dbDriver
   * numDays
   * emailHost
   * emailSender
   * emailReceipients (space delimited list)
   * emailSubject
   *
   * @param args
   */
  public static void main(String[] args) {
    def config = new ConfigSlurper().parse(new File(args[0]).toURL())
    def sql = Sql.newInstance(config.dbUrl, config.dbUser, config.dbPasswd ?: "", config.dbDriver)
    def report = new AuditReport(config, sql);
    def data = report.collectData()
    def output = "Report for the last ${config.numDays} day(s)\n"
    output += report.genTopUsersReport(data)
    output += report.genQueryReport(data)
    report.email(output)
  }
}
