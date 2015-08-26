package net.shrine.dao.squeryl

import org.squeryl.Table
import org.squeryl.Query
import org.squeryl.Schema

/**
 * @author clint
 * @date Dec 22, 2014
 */
trait AbstractSquerylDaoTest {
  import SquerylEntryPoint._
  
  type MyTables <: Schema
  
  def tables: MyTables

  final protected def allRowsQuery[A, B](table: Table[A])(transform: A => B): Query[B] = from(table)(row => select(transform(row)))
  
  final protected def list[T](q: Query[T]): Seq[T] = q.toSeq
  
  final protected def first[T](q: Query[T]): T = q.single
  
  final protected def afterCreatingTables(body: => Any): Unit = afterCreatingTablesReturn(body)
  
  final protected def afterCreatingTablesReturn[T](body: => T): T = {
    inTransaction {
      tables.drop
        
      tables.create
        
      body
    }
  }
}