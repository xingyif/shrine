package net.shrine.adapter

import scala.ref.WeakReference

/**
 * @author clint
 * @date Dec 18, 2013
 * 
 * A container for a collection of things, all of which are held onto weakly,
 * via scala.ref.WeakReferences.  This allows keeping a list of references, 
 * say to Adapters that must be shut down, without this collection preventing
 * the referenced objects from being GC'd.
 */
final case class WeakSet[T <: AnyRef](things: Seq[WeakReference[T]]) {
  def :+(t: T): WeakSet[T] = WeakSet(things :+ WeakReference(t))
  
  def foreach(f: T => Any): Unit = values.foreach(f)
  
  def isEmpty = things.isEmpty
  
  def values: Seq[T] = things.collect { case WeakReference(t) => t }
  
  override def toString = values.mkString("WeakSet(", ",", ")")
}

object WeakSet {
  private val EMPTY = new WeakSet[Nothing](Nil)
  
  def empty[T <: AnyRef] = EMPTY.asInstanceOf[WeakSet[T]]
}