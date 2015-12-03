package net.shrine.integration

import net.shrine.util.SEnum

final class NodeName(override val name: String) extends NodeName.Value

object NodeName extends SEnum[NodeName] {
  val ROOT = new NodeName("ROOT")
  val A = new NodeName("A")
  val B = new NodeName("B")
  val C = new NodeName("C")
  val D = new NodeName("D")
  val E = new NodeName("E")
  val F = new NodeName("F")
  val G = new NodeName("G")
  val I = new NodeName("I")
  val J = new NodeName("J")
  val K = new NodeName("K")
  val U = new NodeName("U")
  val V = new NodeName("V")
  val W = new NodeName("W")
  val X = new NodeName("X")
  val Y = new NodeName("Y")
  val Z = new NodeName("Z")
}
