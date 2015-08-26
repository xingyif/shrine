package net.shrine.protocol.query

/**
 * @author clint
 * @date Dec 11, 2012
 */
object ExpressionHelpers {
  private[query] def is[E: Manifest](x: AnyRef) = manifest[E].runtimeClass.isAssignableFrom(x.getClass)
}