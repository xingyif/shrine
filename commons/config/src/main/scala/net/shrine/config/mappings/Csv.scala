package net.shrine.config.mappings

import au.com.bytecode.opencsv.CSVReader
import java.io.Reader
import scala.collection.mutable.Buffer
import scala.collection.mutable.ArrayBuffer
import scala.collection.AbstractIterator
import scala.util.control.NonFatal

object Csv {
  private def makeCsvReader(reader: Reader): CSVReader = {
    //Post-reader params are:
    //seperator char
    //quote char
    //escape char
    //NB: escape char can't be the default, which is '\', since that char is all over i2b2 paths. #windowsfetish :(
    //Use '`' instead, as that isn't present in the Shrine ontology (that we know of) :\
    new CSVReader(reader, ',', '"', '`')
  }

  def slurp(reader: Reader): Seq[(String, String)] = {
    val csvReader = makeCsvReader(reader)

    val lines: Buffer[(String, String)] = new ArrayBuffer

    try {
      var rawLine = csvReader.readNext()

      while (rawLine != null) {
        val Array(shrine, i2b2) = rawLine

        lines += (shrine -> i2b2)

        rawLine = csvReader.readNext()
      }
    } finally { csvReader.close() }

    lines
  }

  def lazySlurp(reader: Reader): Iterator[(String, String)] = {
    val csvReader = makeCsvReader(reader)

    new AbstractIterator[(String, String)] {
      private[this] var lastLine = csvReader.readNext()
      
      private[this] var lineNumber = 1
      
      override def hasNext: Boolean = lastLine != null

      override def next(): (String, String) = closeOnException {
        val result = parse(lastLine)

        advance()

        result
      }

      private[this] def advance(): Unit = {
        lastLine = csvReader.readNext()
        
        lineNumber += 1

        if (!hasNext) { csvReader.close() }
      }

      private[this] def closeOnException[T](f: => T): T = {
        try { f }
        catch { case NonFatal(e) => csvReader.close(); throw e }
      }

      private[this] def parse(line: Array[String]): (String, String) = {
        require(line.size == 2, s"Line $lineNumber: Expected two 'columns' in csv line, but got $line")

        val Array(shrine, i2b2) = line

        shrine -> i2b2
      }
    }
  }
}