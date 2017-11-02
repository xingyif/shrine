package net.shrine.protocol

import javax.xml.datatype.XMLGregorianCalendar

/**
 * @author Bill Simons
 * @since 4/2/12
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */

/**
  *
  * @param queryInstanceId
  * @param queryMasterId
  * @param userId
  * @param groupId
  * @param startDate
  * @param endDate - will be None if the query has not yet finished in a ReadQueryInstanceResponse .
  */
final case class QueryInstance (
                                  queryInstanceId: String,
                                  queryMasterId: String,
                                  userId: String,
                                  groupId: String,
                                  startDate: XMLGregorianCalendar,
                                  endDate: Option[XMLGregorianCalendar]) {
 
  def withId(newId: String): QueryInstance = this.copy(queryInstanceId = newId)

  override def hashCode: Int =  41 * (41 + queryInstanceId.hashCode) + queryMasterId.hashCode

  override def equals(other: Any): Boolean  = {
    other match {
      case that: QueryInstance =>
        queryInstanceId == that.queryInstanceId &&
        queryMasterId == that.queryMasterId
      case _ => false
    }
  }
}

object QueryInstance {
  def apply(
             queryInstanceId: String,
             queryMasterId: String,
             userId: String,
             groupId: String,
             startDate: XMLGregorianCalendar,
             endDate: XMLGregorianCalendar): QueryInstance = new QueryInstance(queryInstanceId, queryMasterId, userId, groupId, startDate, Some(endDate))
}