package net.shrine.utilities.batchquerier.csv

import net.shrine.protocol.query.Expression
import javax.xml.datatype.XMLGregorianCalendar
import scala.concurrent.duration.Duration
import net.shrine.utilities.batchquerier.BatchQueryResult

/**
 * @author clint
 * @date Oct 8, 2013
 */
final case class CsvRow(name: String, institution: String, status: String, count: Long, elapsedInSeconds: String, meanElapsedInSeconds: String, numQueriesPerformed: Int, expressionXml: String)
