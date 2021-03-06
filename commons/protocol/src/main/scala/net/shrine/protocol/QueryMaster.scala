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
final case class QueryMaster (
                               queryMasterId: String, //Outside of tests, this is always the networkQueryId as a string
                               networkQueryId: Long,
                               name: String,
                               userId: String,
                               groupId: String,
                               createDate: XMLGregorianCalendar,
//                               held: Option[Boolean] = None, //todo field should be removed, along with supporting code and tests. It's never used.
                               flagged: Option[Boolean] = None,
                               flagMessage: Option[String] = None)