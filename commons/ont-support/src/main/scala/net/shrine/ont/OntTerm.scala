package net.shrine.ont

/**
 * @author clint
 * @date Jun 10, 2014
 */
final case class OntTerm(parts: Seq[String]) {
  import OntTerm._
  
  override def toString: String = parts.mkString(twoSlashes, forwardSlash.toString, forwardSlash.toString)
  
  def size: Int = parts.size
  
  def startsWith(other: OntTerm): Boolean = {
    parts.take(other.size) == other.parts
  }
  
  def endsWith(other: OntTerm): Boolean = {
    parts.takeRight(other.size) == other.parts
  }
  
  def parent: Option[OntTerm] = size match {
    case 0 | 1 => None
    case _ => Some(OntTerm(parts.dropRight(1)))
  }
  
  lazy val ancestors: Seq[OntTerm] = {
    //Return a vector to allow fast access to the last ancestor,
    //which helps isRelativeOf
    val parentSeq = parent.toVector
    
    parentSeq ++ (for {
      p <- parentSeq
      gp <- p.ancestors
    } yield gp)
  }

  private lazy val ancestorSet = ancestors.toSet
  
  def isAncestorOf(possibleChild: OntTerm): Boolean = {
    possibleChild.ancestorSet.contains(this)
  }
  
  def isDescendantOf(possibleParent: OntTerm): Boolean = {
    this.ancestorSet.contains(possibleParent)
  }
  
  def isRelativeOf(possibleRelative: OntTerm): Boolean = {
    def oldestAncestor(t: OntTerm) = t.ancestors.lastOption.getOrElse(t)
    
    oldestAncestor(possibleRelative) == oldestAncestor(this)
  }
  
  lazy val hLevel: Int = ancestors.size
}

object OntTerm {
  
  def apply(s: String): OntTerm = new OntTerm(s.split(s"$twoSlashes+").filter(!_.isEmpty))
  
  val forwardSlash = '\\'
  
  val twoSlashes = s"$forwardSlash$forwardSlash"
    
  val shrinePrefix = """\\SHRINE\SHRINE\"""
}