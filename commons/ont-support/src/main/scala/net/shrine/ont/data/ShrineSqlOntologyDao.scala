package net.shrine.ont.data

import net.shrine.ont.messaging.Concept
import scala.io.Source
import scala.util.matching.Regex
import scala.util.matching.Regex.Match
import java.io.InputStream

/**
 * @author Clint Gilbert
 * @date Feb 8, 2012
 * 
 */
final class ShrineSqlOntologyDao(val file: InputStream) extends OntologyDao {
  require(file != null)
  
  override def ontologyEntries: Iterable[Concept] = {
    //Matches VALUES (99, '<full term>', 'synonym', '<is_synonym>'
    //where is_synonym is 'Y' or 'N'
    val pathAndSynonymRegex = """VALUES\s+\(\d+,\s+'(.+?)',\s+'(.+?)',\s+'(\w)',\s+'(.+?)',\s+(NULL|'.*?'),\s+(NULL|'(.*?)')""".r
    
    def mungeBaseCode(rawBaseCode: String): Option[String] = {
      val icd9BaseCodePrefix = """SHRINE|ICD9:"""
        
      def isNotNull(s: String) = s != "NULL"
      
      def isNotEmpty(s: String) = !s.isEmpty
        
      def isAcceptable(s: String): Boolean = !s.isEmpty && s != "NULL" && s.startsWith(icd9BaseCodePrefix)
      
      Option(rawBaseCode).filter(isAcceptable).map(_.drop(icd9BaseCodePrefix.size))
    }
    
    def toConcept(termMatch: Match): Concept = {
      /*val synonym = termMatch.group(3) match {
        case "Y" => Option(termMatch.group(2))
        case "N" => None
      }*/
      
      //Ignore synonym_cd (y/n) field.
      //NB: Do this because medications are stored as coded names (..\CV000\CV350\203144\, etc)
      //but with synonyms containing human-readable names for the drugs ('pravastatin sodium', etc).
      //Crucially, synonym_cd is 'N' in this case (not sure why), and we don't want those synonyms to
      //be ignored.  This has the effect of making medications' names available when searching an index
      //of terms, and also makes the relevance ranking "better", in that simple terms "male", "female"
      //are higher up the list.  Some terms now have a redundant synonym now, but this is an ok tradeoff. 
      val synonym = Option(termMatch.group(2))

      val rawPath = termMatch.group(1)
      
      val rawBaseCode = termMatch.group(7)
      
      //Need to add '\\SHRINE' that's missing from the SQL file, but needed by i2b2 
      Concept("""\\SHRINE""" + rawPath, synonym, mungeBaseCode(rawBaseCode))
    }
    
    parseWith(pathAndSynonymRegex, toConcept)
  }
  
  private def parseWith(regex: Regex, parser: Match => Concept): Iterable[Concept] = {
    
    def parseLine(line: String): Option[Concept] = {
      val result = regex.findFirstMatchIn(line).map(parser)
      
      if(result.isEmpty) {
        println("Failed to parse line: " + line)
      }
      
      result
    }
    
    def noEmptyLines(line: String) = line != null && line.trim != ""
      
    def mungeSingleQuotes(line: String) = line.replace("''", "'")
    
    val source = Source.fromInputStream(file)
    
    source.getLines.filter(noEmptyLines)/*.map(mungeSingleQuotes)*/.flatMap(parseLine).toIterable
  }
}
