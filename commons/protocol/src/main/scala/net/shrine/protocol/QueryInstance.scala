package net.shrine.protocol

import javax.xml.datatype.XMLGregorianCalendar

/**
 * @author Bill Simons
 * @date 4/2/12
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final case class QueryInstance (
    val queryInstanceId: String,
    val queryMasterId: String,
    val userId: String,
    val groupId: String,
    val startDate: XMLGregorianCalendar,
    val endDate: XMLGregorianCalendar) {
 
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