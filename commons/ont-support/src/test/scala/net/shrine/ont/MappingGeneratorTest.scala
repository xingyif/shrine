package net.shrine.ont

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Jun 11, 2014
 */
final class MappingGeneratorTest extends ShouldMatchersForJUnit {
  @Test
  def testGenerate: Unit = {
    val shrineTerm = OntTerm(Seq("SHRINE", "SHRINE", "FOO", "BAR"))

    val i2b2Term = OntTerm(Seq("i2b2", "i2b2", "FOO", "BAR"))

    val shrineFoo = shrineTerm.parent.get
    val shrine2 = shrineFoo.parent.get
    val shrine1 = shrine2.parent.get

    {
      val results = MappingGenerator.generate(shrineTerm, i2b2Term, None)

      results should equal(Seq(
        shrineTerm -> i2b2Term,
        shrineFoo -> i2b2Term,
        shrine2 -> i2b2Term,
        shrine1 -> i2b2Term))
    }

    {
      val results = MappingGenerator.generate(shrineTerm, i2b2Term, Some(1))

      results should equal(Seq(
        shrineTerm -> i2b2Term,
        shrineFoo -> i2b2Term,
        shrine2 -> i2b2Term))
    }
  }

  @Test
  def testGenerateIterableOfOntTermPairs: Unit = {
    val xyza = OntTerm("""\\X\Y\Z\A\""")
    val xyz = OntTerm("""\\X\Y\Z\""")
    val xy = OntTerm("""\\X\Y\""")
    val x = OntTerm("""\\X\""")

    val abc = OntTerm("""\\A\B\C\""")
    val abca1 = OntTerm("""\\A\B\C\A1\""")
    val abca2 = OntTerm("""\\A\B\C\A2\""")

    val mappings = Map(
      xyza -> Set(abca1, abca2),
      xyz -> Set(abc))

    val pairs: Seq[(OntTerm, OntTerm)] = for {
      (shrine, locals) <- mappings.toSeq
      local <- locals
    } yield shrine -> local

    {
      val generated = MappingGenerator.generate(pairs.iterator, None)

      generated should equal(Map(
        xyza -> Set(abca1, abca2),
        xyz -> Set(abca1, abca2, abc),
        xy -> Set(abca1, abca2, abc),
        x -> Set(abca1, abca2, abc)))
    }

    {
      val generated = MappingGenerator.generate(pairs.iterator, Some(2))

      generated should equal(Map(
        xyza -> Set(abca1, abca2),
        xyz -> Set(abca1, abca2, abc)))
    }
  }

  @Test
  def testGenerateIterableOfOntTermPairsNonAncestor: Unit = {
    val xyza = OntTerm("""\\X\Y\Z\A\""")
    val xyz = OntTerm("""\\X\Y\Z\""")
    val xy = OntTerm("""\\X\Y\""")
    val x = OntTerm("""\\X\""")

    val pqr = OntTerm("""\\P\Q\R\""")
    val abca1 = OntTerm("""\\A\B\C\A1\""")
    val abca2 = OntTerm("""\\A\B\C\A2\""")

    val mappings = Map(
      xyza -> Set(abca1, abca2),
      xyz -> Set(pqr))

    val pairs: Seq[(OntTerm, OntTerm)] = for {
      (shrine, locals) <- mappings.toSeq
      local <- locals
    } yield shrine -> local

    {
      val generated = MappingGenerator.generate(pairs.iterator, None)

      generated should equal(Map(
        xyza -> Set(abca1, abca2),
        xyz -> Set(abca1, abca2, pqr),
        xy -> Set(abca1, abca2, pqr),
        x -> Set(abca1, abca2, pqr)))
    }

    {
      val generated = MappingGenerator.generate(pairs.iterator, Some(2))

      generated should equal(Map(
        xyza -> Set(abca1, abca2),
        xyz -> Set(abca1, abca2, pqr)))
    }
  }
}