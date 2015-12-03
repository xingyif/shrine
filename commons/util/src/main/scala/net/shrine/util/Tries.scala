package net.shrine.util

import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.collection.generic.CanBuildFrom

/**
 * Helpers for working with scala.util.Try
 * @author clint
 * @since Oct 18, 2012
 */
object Tries {
  def toTry[T](o: Option[T])(ex: => Throwable): Try[T] = o match {
    case Some(t) => Success(t)
    case None => Failure(ex)
  }

  /**
   * Implicits to allow mixing Options and Tries in for-comprehensions
   */
  object Implicits {
    import scala.language.implicitConversions

    implicit def try2Option[T](o: Try[T]): Option[T] = o.toOption
  }

  /**
   * Turns an Option[Try[T]] into a Try[Option[T]], a la Future.sequence.
   */
  def sequence[T](tryOption: Option[Try[T]]): Try[Option[T]] = tryOption match {
    case Some(attempt) => attempt.map(Option(_))
    case None => Try(None)
  }

  /**
   * Turns a Traversable[Try[T]] into a Try[Traversable[T]], a la Future.sequence.
   * Uses CanBuildFrom magic to ensure that the subtype of Traversable passed in is the
   * same subtype of Traversable returned, and that this is verifiable at compile-time.
   *
   * NB: If *any* of the input Tries are Failures, then the first Failure is returned;
   * this can drop subsequent Failures if there are more than one.
   */
  import scala.language.higherKinds

  def sequence[A, C[+A] <: Traversable[A]](attempts: C[Try[A]])(implicit cbf: CanBuildFrom[C[A], A, C[A]]): Try[C[A]] = {
    //Simplified implementation, like Future.sequence
    val z = Try(cbf())

    attempts.foldLeft(z) { (accAttempt, attempt) =>
      for {
        builder <- accAttempt
        t <- attempt
      } yield (builder += t)
    }.map(_.result())
  }
}