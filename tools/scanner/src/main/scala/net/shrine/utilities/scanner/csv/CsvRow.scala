package net.shrine.utilities.scanner.csv

import net.shrine.utilities.scanner.Disposition

/**
 * @author clint
 * @date Mar 21, 2013
 */
final case class CsvRow(disposition: Disposition, term: String)