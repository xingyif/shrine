package net.shrine.utilities.mapping.compression

import net.shrine.ont.OntTerm

/**
 * @author clint
 * @date Jul 31, 2014
 */
object Compressor {
  def compress(mappings: Map[OntTerm, Set[OntTerm]]): Map[OntTerm, Set[OntTerm]] = {
    for {
      (shrineTerm, i2b2Terms) <- mappings
    } yield {
      //O(n) algorithm to "Compress" sets of terms by taking advantage of known (and unchanging)
      //implementation details of i2b2.  This is necessary because i2b2 can't/won't optimize incoming
      //queries based on i2b2's own implementation details, so we have to.  
      //
      //"Compress" a set of terms by finding the "highest" ancestors from a set of terms.  
      //Given
      //Set(uvw, xyz, abc, abcd, abcx, abcde)
      //"Compress" this to
      //Set(uvw, xyz, abc)
      //abc,abcd,abcx,abcde all descend from abc, so collapse all of them to just abc.
      //
      //Works in linear time as follows:
      //Keep a set of bins that hold the "highest" term in a particular lineage.  For example, abc is
      //higher than (an ancestor of) abcd.  A set of terms S is related if there's a common ancestor 
      //in S of all the terms in S.  That is, Set(abc, abcx, abcde) is related.  
      //A) For each i2b2 term:
      //   1)find a bin for that term to go in (or create one)
      //   2) replace the term in that bin with the new term if the new term is an ancestor of the 
      //      term currently in the bin (terms go in a bin if they're related to the other terms in 
      //      that bin)
      //B) produce the terms from each bin as a Set.  
      val newI2b2Terms: Set[OntTerm] = {
        val allBins: Set[Bin] = i2b2Terms.foldLeft(Set.empty[Bin]) { (bins, term) =>
          val binForTerm = bins.find(_.accepts(term)).getOrElse(Bin(term))

          val withoutOldBin = bins - binForTerm

          val newBin = binForTerm + term

          withoutOldBin + newBin
        }

        allBins.map(_.highest)
      }
      shrineTerm -> newI2b2Terms
    }
  }

  private final case class Bin(highest: OntTerm) {
    def accepts(term: OntTerm): Boolean = highest.isRelativeOf(term)

    def +(term: OntTerm): Bin = {
      if (term.isAncestorOf(highest)) { Bin(term) }
      else { this }
    }
  }
}