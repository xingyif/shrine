package net.shrine.utilities.batchquerier.commands

import java.io.File
import java.io.FileReader
import net.shrine.log.Loggable

import scala.util.Failure
import scala.util.Success
import scala.xml.XML
import net.shrine.protocol.query.QueryDefinition
import net.shrine.utilities.commands.>>>
import net.shrine.util.XmlUtil

/**
 * @author clint
 * @date Sep 16, 2013
 */
object ReadXmlQueryDefs extends (File >>> Iterable[QueryDefinition]) with Loggable {
  override def apply(file: File): Iterable[QueryDefinition] = {
    val xml = XML.load(new FileReader(file))
    
    val attempts = for {
      queryXml <- xml \ "queryDefinition"
      if !queryXml.isAtom
    } yield (queryXml, QueryDefinition.fromXml(queryXml))
    
    val (successes, failures) = attempts.partition { case (_, attempt) => attempt.isSuccess } 
    
    failures.collect { case (queryXml, Failure(e)) => (queryXml, e) }.foreach { case (queryXml, e) =>
      warn(s"Ignoring malformed query definition XML '$queryXml'", e)
    }
    
    val (_, queryDefAttempts) = attempts.unzip
      
    queryDefAttempts.collect { case Success(queryDef) => queryDef } 
  }
  
  override def toString = "ReadXmlQueryDefs"
}