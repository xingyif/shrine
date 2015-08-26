package net.shrine.ont.data

/**
 * @author clint
 * @date Jan 22, 2014
 */
trait HasShrineSqlStream {
  def shrineSqlStream = this.getClass.getClassLoader.getResourceAsStream("testShrineWithSyns.sql") 
}