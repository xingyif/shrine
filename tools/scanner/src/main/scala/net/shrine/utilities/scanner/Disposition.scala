package net.shrine.utilities.scanner

import net.shrine.util.SEnum

/**
 * @author clint
 * @date Mar 21, 2013
 */
final class Disposition(val name: String) extends Disposition.Value

object Disposition extends SEnum[Disposition] {
  val ShouldNotHaveBeenMapped = new Disposition("ShouldNotHaveBeenMapped")
  val ShouldHaveBeenMapped = new Disposition("ShouldHaveBeenMapped")
  val NeverFinished = new Disposition("NeverFinished")
  val Failed = new Disposition("Failed")
}