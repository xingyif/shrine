package net.shrine.protocol

/**
 * @author clint
 * @date Nov 30, 2012
 */
trait HasQueryResults {
  def results: Seq[QueryResult]
}