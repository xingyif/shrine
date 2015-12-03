package net.shrine.adapter.dao.model.squeryl

import net.shrine.protocol.ResultOutputType
import java.sql.Timestamp
import net.shrine.protocol.QueryResult
import net.shrine.adapter.dao.model.QueryResultRow
import net.shrine.dao.DateHelpers
import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.util.XmlDateHelper
import org.squeryl.KeyedEntity
import org.squeryl.annotations.Column

/**
 * @author clint
 * @date May 28, 2013
 */
case class SquerylQueryResultRow (
    @Column(name = "ID")
    id: Int,
    @Column(name = "LOCAL_ID")
    localId: Long,
    @Column(name = "QUERY_ID")
    queryId: Int,
    @Column(name = "TYPE")
    resultType: String,
    @Column(name = "STATUS")
    status: String,
    @Column(name = "TIME_ELAPSED")
    elapsed: Option[Long], //Will be None for error results
    @Column(name = "LAST_UPDATED")
    lastUpdated: Timestamp) extends KeyedEntity[Int] {

  def this(
      id: Int,
      localId: Long,
      queryId: Int,
      resultType: ResultOutputType,
      status: QueryResult.StatusType,
      elapsed: Option[Long], //Will be None for error results
      lastUpdated: XMLGregorianCalendar) = this(id, localId, queryId, resultType.toString, status.toString, elapsed, DateHelpers.toTimestamp(lastUpdated))
  
  //NB: For Squeryl, ugh :(
  def this() = this(0, 0L, 0, ResultOutputType.ERROR, QueryResult.StatusType.Error, Option(0L), XmlDateHelper.now)
  
  def toQueryResultRow(implicit breakdownTypes: Set[ResultOutputType]) = QueryResultRow(id, localId, queryId, ResultOutputType.valueOf(breakdownTypes)(resultType).get, QueryResult.StatusType.valueOf(status).get, elapsed, DateHelpers.toXmlGc(lastUpdated))
}