package net.shrine.utilities.scanner

import net.shrine.utilities.commands.>>>
import net.shrine.utilities.commands.WriteTo
import net.shrine.utilities.scanner.commands.OutputCsv
import net.shrine.utilities.scanner.commands.ToCsvData
import java.io.File


/**
 * @author clint
 * @date Mar 21, 2013
 */
object Output {
  def to(file: File): ScanResults >>> Unit = {
    ToCsvData.andThen(OutputCsv).andThen(WriteTo(file))
  }
}