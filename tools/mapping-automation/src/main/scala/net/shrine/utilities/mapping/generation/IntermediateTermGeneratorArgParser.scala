package net.shrine.utilities.mapping.generation

import net.shrine.utilities.scallop.AbstractArgParser
import net.shrine.utilities.mapping.InputOutputFileArgParser

/**
 * @author clint
 * @date Jul 21, 2014
 */
final case class IntermediateTermGeneratorArgParser(override val args: Seq[String]) extends InputOutputFileArgParser(args) {
  val minHLevel = opt[Int](short = 'h', name = "min-hlevel", required = false)
}