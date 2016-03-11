package net.shrine.adapter.dao.model.squeryl

import java.sql.Timestamp
import net.shrine.log.Loggable
import net.shrine.protocol.query.{QueryDefinition, Term, Expression}
import net.shrine.util.{Tries, XmlDateHelper, StringEnrichments}
import net.shrine.adapter.dao.model.ShrineQuery
import net.shrine.dao.DateHelpers
import javax.xml.datatype.XMLGregorianCalendar
import org.squeryl.KeyedEntity
import org.squeryl.annotations.Column
import scala.util.Try
import scala.util.control.NonFatal
import org.squeryl.annotations.Row
import org.squeryl.annotations.FieldToColumnCorrespondanceMode
import scala.xml.NodeSeq
import scala.util.Success
import scala.util.Failure

/**
 * @author clint
 * @since May 28, 2013
 */
//@Row(fieldToColumnCorrespondanceMode=FieldToColumnCorrespondanceMode.EXPLICIT) 
case class SquerylShrineQuery(
    @Column(name = "ID")
    id: Int,
    @Column(name = "LOCAL_ID")
    localId: String,
    @Column(name = "NETWORK_ID")
    networkId: Long,
    @Column(name = "QUERY_NAME")
    name: String,
    @Column(name = "USERNAME")
    username: String,
    @Column(name = "DOMAIN")
    domain: String,
    @Column(name = "QUERY_EXPRESSION")
    queryExpr: Option[String],
    @Column(name = "DATE_CREATED")
    dateCreated: Timestamp,
    @Column(name = "FLAGGED")
    isFlagged: Boolean,
    @Column(name = "FLAG_MESSAGE")
    flagMessage: Option[String],
    @Column(name = "HAS_BEEN_RUN")
    hasBeenRun: Boolean,
    @Column(name="QUERY_XML")
    queryXml: Option[String]) extends KeyedEntity[Int] with Loggable {

    def this(id: Int,
            localId: String,
            networkId: Long,
            username: String,
            domain: String,
            dateCreated: XMLGregorianCalendar,
            isFlagged: Boolean,
            flagMessage: Option[String],
            hasBeenRun: Boolean,
            queryDefinition:QueryDefinition) = this(id,
                                                    localId,
                                                    networkId,
                                                    queryDefinition.name,
                                                    username,
                                                    domain,
                                                    queryDefinition.expr.map(_.toXmlString),
                                                    DateHelpers.toTimestamp(dateCreated),
                                                    isFlagged,
                                                    flagMessage,
                                                    hasBeenRun,
                                                    Option(queryDefinition.toXmlString))

  //NB: For Squeryl, ugh :(
  def this() = this(0, "", 0L, "", "", XmlDateHelper.now, false, None, false, QueryDefinition("", Term("foo")))

  final def toShrineQuery: ShrineQuery = {

    val queryDefinition: QueryDefinition = {
      import StringEnrichments._
      
      val queryXmlAttempt: Try[NodeSeq] = Tries.toTry(queryXml.map(_.tryToXml)) {
        new Exception(s"Couldn't parse '$queryXml' as XML")
      }.flatten
      
      queryXmlAttempt.flatMap(QueryDefinition.fromXml).recover {
        case NonFatal(e) => {
          debug(s"queryXml of '$queryXml' unusable, attempting to use old queryExpr field instead.", e)
      
          queryExpr match {
            case Some(exprXml) => QueryDefinition(name, Expression.fromXml(exprXml).get)
            case None => throw new Exception(s"No query expression xml to parse")
          }
        }
      }.get.copy(unTrimmedName = name)
    }

    ShrineQuery(id, localId, networkId, queryDefinition.name, username, domain, DateHelpers.toXmlGc(dateCreated), isFlagged, flagMessage, queryDefinition)
  }
}
