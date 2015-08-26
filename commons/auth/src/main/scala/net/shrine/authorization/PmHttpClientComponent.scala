package net.shrine.authorization

import net.shrine.client.Poster

/**
 * @author clint
 * @date Apr 5, 2013
 */
trait PmHttpClientComponent {
  val pmPoster: Poster
  
  def callPm(payload: String) = pmPoster.post(payload)
}