package net.shrine.utilities.scanner

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import scala.concurrent.ExecutionContext

/**
 * @author clint
 * @date Nov 21, 2013
 */
object SingleThreadExecutionContext {
  private val executor: ExecutorService = Executors.newSingleThreadExecutor

  object Implicits {
    implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executor)
  }

  def shutdown() {
    try {
      executor.shutdown()

      executor.awaitTermination(5, TimeUnit.SECONDS)
    } finally {
      executor.shutdownNow()
    }
  }
}