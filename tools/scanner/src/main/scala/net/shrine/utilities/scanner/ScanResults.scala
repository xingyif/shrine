package net.shrine.utilities.scanner

/**
 * @author clint
 * @date Mar 5, 2013
 */
final case class ScanResults(
    //Terms that we expected to BE mapped, but were NOT mapped
    shouldHaveBeenMapped: Set[String],
    //Terms that we expected to NOT be mapped, but ARE mapped
    shouldNotHaveBeenMapped: Set[String],
    //Terms that never completed after some timeout period
    neverFinished: Set[String],
    //Terms for which there were query failures (thrown exceptions)
    failed: Set[String])
    
object ScanResults {
  val empty = ScanResults(Set.empty, Set.empty, Set.empty, Set.empty)
}