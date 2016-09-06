package net.shrine.slick

/**
  * Designates initialization heavy objects that should be warmed up in order
  * to improve user experience and front end performance
  */
trait NeedsWarmUp {
  def warmUp(): Unit
}
