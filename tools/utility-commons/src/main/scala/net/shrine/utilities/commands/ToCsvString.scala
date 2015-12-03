package net.shrine.utilities.commands

import java.io.StringWriter
import au.com.bytecode.opencsv.CSVWriter

/**
 * @author clint
 * @date Sep 18, 2013
 */
trait ToCsvString[T <: Product] extends (Iterable[T] >>> String) {
  def headerNames: Option[Seq[String]] = None
  
  override def apply(csvData: Iterable[T]): String = {
    val stringWriter = new StringWriter

    val csvWriter = new CSVWriter(stringWriter)

    try {
      headerNames.foreach(headers => csvWriter.writeNext(headers.toArray))
      
      val rowsAsArraysOfFields = csvData.map(_.productIterator.map(String.valueOf).toArray)

      rowsAsArraysOfFields.foreach(csvWriter.writeNext)
    } finally {
      csvWriter.close()
    }

    stringWriter.toString.trim
  }
  
  override def toString = "ToCsvString"
}