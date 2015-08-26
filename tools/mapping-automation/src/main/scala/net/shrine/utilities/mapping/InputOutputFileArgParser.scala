package net.shrine.utilities.mapping

import net.shrine.utilities.scallop.AbstractArgParser

/**
 * @author clint
 * @date Jul 18, 2014
 */
class InputOutputFileArgParser(override val args: Seq[String]) extends AbstractArgParser(args) {
  final val inputFile = opt[String](short = 'i', required = true)

  final val outputFile = opt[String](short = 'o', required = true)
}

object InputOutputFileArgParser {
  def apply(args: Seq[String]): InputOutputFileArgParser = new InputOutputFileArgParser(args)
}