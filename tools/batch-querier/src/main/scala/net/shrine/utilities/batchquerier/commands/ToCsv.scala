package net.shrine.utilities.batchquerier.commands

import net.shrine.utilities.batchquerier.csv.CsvRow
import net.shrine.utilities.commands.ToCsvString

/**
 * @author clint
 * @date Sep 16, 2013
 */
object ToCsv extends ToCsvString[CsvRow] with App {
  override def toString = "ToCsv"

  //Use reflection to make CSV headers.  Not great, but not bad either.
  override def headerNames: Option[Seq[String]] = {
    def unCamelCase(s: String): String = {
      //regex magic from http://stackoverflow.com/a/8837360
      s.
        replaceAll("([A-Z][a-z]+)", " $1"). // Words beginning with UC
        replaceAll("([A-Z][A-Z]+)", " $1"). // "Words" of only UC
        replaceAll("([^A-Za-z ]+)", " $1"). // "Words" of non-letters
        trim
    }
    
    Option {
      import scala.reflect.runtime.universe._

      typeOf[CsvRow].members.collect {
        case m: MethodSymbol if m.isCaseAccessor => unCamelCase(m.name.toString).capitalize
      }.toSeq.reverse
    }
  }
}