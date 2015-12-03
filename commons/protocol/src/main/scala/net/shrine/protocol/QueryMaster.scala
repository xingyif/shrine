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
final case class QueryMaster (
                               queryMasterId: String, networkQueryId: Long,
                               name: String,
                               userId: String,
                               groupId: String,
                               createDate: XMLGregorianCalendar,
                               held: Option[Boolean] = None,
                               flagged: Option[Boolean] = None,
                               flagMessage: Option[String] = None)