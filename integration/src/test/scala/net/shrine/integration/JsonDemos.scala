package net.shrine.integration

import java.io.InputStream

import jawn.{AsyncParser, Parser, ast}
import jawn.ast.JValue
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable
import scala.util.Try

/**
  * @by ty
  * @since 1/24/17
  */
@RunWith(classOf[JUnitRunner])
class JsonDemos extends FlatSpec with Matchers {
  val parser     : AsyncParser[JValue] = ast.JParser.async(mode = AsyncParser.UnwrapArray)
  val exampleJson: String              =
    """
      |{
      |  "a" : {
      |         "a" : {"a" : {"a" : "a"}},
      |         "b" : {"b" : {"b" : "b"}}
      |        },
      |  "b" : {"b" : {"b" : {"b" : "b"}}}
      |}
    """.stripMargin
  val jawnJson                         = ast.JObject(mutable.Map(
    "a" -> ast.JObject(mutable.Map(
      "a" -> ast.JObject(mutable.Map("a" -> ast.JObject(mutable.Map("a" -> ast.JString("a"))))),
      "b" -> ast.JObject(mutable.Map("b" -> ast.JObject(mutable.Map("b" -> ast.JString("b"))))))),
    "b" -> ast.JObject(mutable.Map("b" -> ast.JObject(mutable.Map("b" -> ast.JObject(mutable.Map("b" -> ast.JString("b")))))))))

  "The simple, synchronous parser" should "parse the example string" in {
    Parser.parseFromString(exampleJson)(jawn.ast.JawnFacade) shouldBe Try(jawnJson)
  }

  "The async parser" should "parse the file stream and return values as they appear" in {
    val stream: InputStream = getClass.getResourceAsStream("/test_data.json")
    val lines = scala.io.Source.fromInputStream(stream).getLines
      .flatMap(l => parser.absorb(l).right.get)
      .toSeq
    lines.length shouldBe 24
    lines.foreach(_.valueType shouldBe "object")
  }
}
