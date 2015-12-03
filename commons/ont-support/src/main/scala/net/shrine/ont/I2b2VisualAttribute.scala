package net.shrine.ont

import net.shrine.util.SEnum

/**
 * @author clint
 * @date Jun 11, 2014
 */
final case class I2b2VisualAttribute(name: String, abbreviation: String) extends I2b2VisualAttribute.Value

object I2b2VisualAttribute extends SEnum[I2b2VisualAttribute] {
  val Container = I2b2VisualAttribute("Container", "C")
  val Folder = I2b2VisualAttribute("Folder", "F")
  val Multiple = I2b2VisualAttribute("Multiple", "M")
  val Leaf = I2b2VisualAttribute("Leaf", "L")
  val ModifierContainer = I2b2VisualAttribute("Modifier container", "O")
  val ModifierFolder = I2b2VisualAttribute("Modifier folder", "D")
  val ModifierLeaf = I2b2VisualAttribute("Modifier leaf", "R")
  val Active = I2b2VisualAttribute("Active", "A")
  val Inactive = I2b2VisualAttribute("Inactive", "I")
  val Hidden = I2b2VisualAttribute("Hidden", "H")
  val Editable = I2b2VisualAttribute("Editable", "E")

  def fromAbbreviation(abbreviation: String): Option[I2b2VisualAttribute] = {
    values.find(_.abbreviation == abbreviation)
  }
  
  def parse(attrString: String): Set[I2b2VisualAttribute] = {
    for {
      abbreviation <- attrString.toSet.map((_: Char).toString)
      attr <- fromAbbreviation(abbreviation)
    } yield attr
  }
}