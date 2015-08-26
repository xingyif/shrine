package net.shrine.ont

/**
 * @author clint
 * @date Jun 11, 2014
 */
object MappingGenerator {
  def generate(shrineLeafTerm: OntTerm, i2b2LeafTerm: OntTerm, hLevelToStopAt: Option[Int]): Seq[(OntTerm, OntTerm)] = {
    val ancestors = shrineLeafTerm.ancestors
    
    val ancestorsToTake = ancestors.size - hLevelToStopAt.getOrElse(0)
    
    for {
      shrineTerm <- (shrineLeafTerm +: ancestors.take(ancestorsToTake))
    } yield shrineTerm -> i2b2LeafTerm
  }
  
  def generate(shrineToI2b2LeafMappings: Iterator[(OntTerm, OntTerm)], hLevelToStopAt: Option[Int]): Map[OntTerm, Set[OntTerm]] = {
    val mappings = for {
      (shrineLeafTerm, i2b2LeafTerm) <- shrineToI2b2LeafMappings
      mapping <- generate(shrineLeafTerm, i2b2LeafTerm, hLevelToStopAt)
    } yield mapping
    
    mappings.foldLeft(Map.empty[OntTerm, Set[OntTerm]]) { (acc, mapping) =>
      val (shrine, i2b2) = mapping 
      
      val i2b2Terms = acc.getOrElse(shrine, Set.empty) + i2b2
      
      acc + (shrine -> i2b2Terms)
    }
  }
}