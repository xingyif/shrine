package net.shrine.util

/**
  * A simple sequence with a guaranteed first element. This was made for the
  * crypto package, since we want to guarantee that on boot time there is an
  * alias for every entry in the keyStore, however, certs can have more than
  * one alias.
  * @param first the first element
  * @param rest  the possible empty tail
  */
case class NonEmptySeq[+A](first: A, rest: Seq[A]) extends Seq[A] {
  override def length: Int = 1 + rest.length

  override def iterator: Iterator[A] = (first +: rest).iterator
}