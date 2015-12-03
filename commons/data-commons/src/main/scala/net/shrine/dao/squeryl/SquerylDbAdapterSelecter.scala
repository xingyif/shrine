package net.shrine.dao.squeryl

import org.squeryl.internals.DatabaseAdapter
import org.squeryl.adapters.MySQLAdapter
import org.squeryl.adapters.OracleAdapter
import org.squeryl.adapters.MSSQLServer

/**
 * @author clint
 * @date Aug 13, 2013
 */
object SquerylDbAdapterSelecter {
  private val dbTypes: Map[String, () => DatabaseAdapter] = Map(
      "mysql" -> (() => new MySQLAdapter),
      "oracle" -> (() => new OracleAdapter),
      "sqlserver" -> (() => new MSSQLServer)
  )
  
  def determineAdapter(dbType: String): DatabaseAdapter = {
    require(dbType != null)
    
    dbTypes.get(dbType.toLowerCase) match {
      case Some(makeAdapter) => makeAdapter()
      case _ => throw new IllegalArgumentException(s"Unknown DB type '$dbType'; known types are ${ dbTypes.keys.mkString(",") }")
    }
  }
}