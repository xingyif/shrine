package net.shrine.utilities.scanner.commands

import net.shrine.utilities.scanner.csv.CsvRow
import net.shrine.utilities.commands.ToCsvString


/**
 * @author clint
 * @date Mar 21, 2013
 */
case object OutputCsv extends ToCsvString[CsvRow]
