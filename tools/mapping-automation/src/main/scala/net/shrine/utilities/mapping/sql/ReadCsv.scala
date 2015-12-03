package net.shrine.utilities.mapping.sql

import java.io.Reader
import au.com.bytecode.opencsv.CSVReader
import scala.collection.mutable.Buffer
import scala.collection.mutable.ArrayBuffer
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader

/**
 * @author clint
 * @date Jun 13, 2014
 */
final object ReadCsv extends (Reader => Seq[Seq[String]]) {
  override def apply(fileReader: Reader): Seq[Seq[String]] = {
    //Post-reader params are:
    //seperator char
    //quote char
    //escape char
    //NB: escape char can't be the default, which is '\', since that char is all over i2b2 paths. #windowsfetish :(
    //Use '`' instead, as that isn't present in the Shrine ontology (that we know of) :\
    val csvReader = new CSVReader(fileReader, ',', '"', '`')
    
    val lines: Buffer[Seq[String]] = new ArrayBuffer
    
    try {
      var line: Seq[String] = csvReader.readNext()
      
      while(line != null) {
        lines += line
        
        line = csvReader.readNext()
      }
    } finally { csvReader.close() }
    
    lines.toIndexedSeq
  } 

  def fromFile(csvFileName: String): Reader = {
    val file = new File(csvFileName)
    
    require(file.exists, s"Couldn't find file '${file.getCanonicalPath}'")
    
    new FileReader(file)
  }
  
  def fromClasspath(csvFileName: String): Reader = {
    val stream = getClass.getClassLoader.getResourceAsStream(csvFileName)
    
    require(stream != null, s"Couldn't find file '$csvFileName' on the classpath")
    
    new InputStreamReader(stream)
  }
  
  def fromStdIn: Reader = new InputStreamReader(System.in)
}