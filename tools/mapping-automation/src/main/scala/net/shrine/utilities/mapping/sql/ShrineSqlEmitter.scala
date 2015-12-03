package net.shrine.utilities.mapping.sql

import scala.util.Try

/**
 * @author clint
 * @date Jun 13, 2014
 */
final case class ShrineSqlEmitter(dbTableName: String = "SHRINE") {
  def toSql(lines: Seq[Seq[String]]): Seq[String] = {
    require(!lines.isEmpty)
    
    val headers = lines.head.map(_.trim)

    def isSqlNull(s: String) = s.toUpperCase == "NULL"
    def isInt(s: String) = Try(s.toInt).isSuccess
    
    val withQuotes = lines.tail.map(_.map(_.trim)).map { lineParts =>
      lineParts.map { part =>
        if(isSqlNull(part) || isInt(part)) { part }
        else { s"'$part'" }
      }
    }
    
    val columnNames = headers.mkString(", ")
    
    withQuotes.map { lineParts =>
      s"INSERT INTO $dbTableName ($columnNames) VALUES (${lineParts.mkString(", ")});"
    }
  }
}