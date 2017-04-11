package net.shrine.integration

import java.io.InputStream
import jawn.{AsyncParser, Parser, ast}
import jawn.ast.JValue
import rapture.json._
import jsonBackends.jawn._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable.{Map => MMap}
import scala.util.Try

/**
  * @by ty
  * @since 1/24/17
  */
@RunWith(classOf[JUnitRunner])
class JsonDemos extends FlatSpec with Matchers {
  // The async parser will spit back values from an array as it parses them. Can also do whitespace
  // separated json objects
  val parser: AsyncParser[JValue] = ast.JParser.async(mode = AsyncParser.UnwrapArray)
  // Rapture provides the json""" syntax, but the actual parsing is handled by jawn itself
  // The ast that's produced is also a jawn AST object
  // One annoyance is that intellji tries to add the stripMargin thing, which json""" doesn't handle
  val exampleJson =
    json"""
          {"a":{"a":{"a":{"a":"a"}},"b":{"b":{"b":"b"}}},"b":{"b":{"b":{"b":"b"}}}}
      """
  // This is the api jawn normally exposes to build an AST object
  val jawnJson = ast.JObject(
    MMap(
      "a" -> ast.JObject(
        MMap("a" -> ast.JObject(
               MMap("a" -> ast.JObject(MMap("a" -> ast.JString("a"))))),
             "b" -> ast.JObject(
               MMap("b" -> ast.JObject(MMap("b" -> ast.JString("b"))))))),
      "b" -> ast.JObject(MMap("b" -> ast.JObject(
        MMap("b" -> ast.JObject(MMap("b" -> ast.JString("b")))))))
    ))

  val person = Person("Ty", "Coghlan")
  val account = Account(person, -100000000.87) // Student loans man
  val accountJson = Json(account)
  val accountJsonLiteral =
    json"""
          {"person": {"first": "Ty", "last": "Coghlan"}, "money": -100000000.87}
        """
  val extraStuffAccount =
    accountJson ++ json"""{"overdue":true, "randomArray": [0, 1, 2, 3, {"surprise": "boo"}]}"""

  "The simple, synchronous jawn parser" should "parse the example string" in {
    Parser.parseFromString(exampleJson.toBareString)(jawn.ast.JawnFacade) shouldBe Try(
      jawnJson)
  }

  "The async jawn parser" should "parse the file stream and return values as they appear" in {
    val stream: InputStream = getClass.getResourceAsStream("/test_data.json")
    val lines = scala.io.Source
      .fromInputStream(stream)
      .getLines
      .flatMap(l => parser.absorb(l).right.get)
      .toSeq
    lines.foreach(_.valueType shouldBe "object")
    lines.length shouldBe 24
  }

  "Case class serialization" should "be painless" in {
    accountJson shouldBe accountJsonLiteral
    accountJson.as[Account] shouldBe account
    // Can also serialize json that has "extra"
    extraStuffAccount.as[Account] shouldBe account
    // Or if you just want the values themselves
    extraStuffAccount.overdue shouldBe Json(true)
    extraStuffAccount.randomArray(4).surprise.as[String] shouldBe "boo"
    extraStuffAccount match {
      case json""" {"person": $p, "overdue": false} """ =>
        fail("Did not match overdue as expected")
      // Note that you don't need money for the match
      case json""" {"person": $p, "overdue": true } """ =>
        person shouldBe p.as[Person]
      case _ => fail("Did not match as expected")
    }
  }
}

case class Account(person: Person, money: Double)
case class Person(first: String, last: String)
