package net.shrine.json

import java.util.UUID

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import rapture.json._
import jsonBackends.jawn._

/**
  * @by ty
  * @since 2/1/17
  */
@RunWith(classOf[JUnitRunner])
class Structures extends FlatSpec with Matchers {
  val uid       = UUID.randomUUID()
  val startTime = System.currentTimeMillis()
  val i2b2Xml = <ami2b2>suchi2b2, wow</ami2b2>
  val extraXml = <extra><extra2><extra3>lots of extra</extra3></extra2></extra>
  val noiseTerms = NoiseTerms(10, 11, 12)
  val resultCount = 1000
  val breakdown = GenderBreakdown(List(BreakdownProperty("male", 70), BreakdownProperty("female", 30)))
  val flags = List("hey", "what's", "that", "sound")
  val queryJson =
    json"""{
              "queryId": ${uid.toString},
              "topicId": ${uid.toString},
              "userId": ${uid.toString},
              "startTime": $startTime,
              "i2b2QueryText": ${i2b2Xml.toString},
              "extraXml": ${extraXml.toString},
              "queryResults": [
                {
                   "status": "success",
                   "resultId": ${uid.toString},
                   "adapterId": ${uid.toString},
                   "count": $resultCount,
                   "noiseTerms": {
                      "sigma": ${noiseTerms.sigma},
                      "clamp": ${noiseTerms.clamp},
                      "rounding": ${noiseTerms.rounding}
                   },
                   "i2b2Mapping": ${i2b2Xml.toString},
                   "flags": $flags,
                   "breakdowns": [
                     {
                       "category": "gender",
                       "results": [
                         {
                           "name": "male",
                           "count": 70
                         },
                         {
                           "name": "female",
                           "count": 30
                         }
                       ]
                     }
                   ]
                }
              ]
           }
    """

  "query" should "be parsed as the query structure" in {
    queryJson.is[Query] shouldBe true
    val query = queryJson.as[Query]
    query.json shouldBe queryJson
    List(query.userId, query.topicId, query.queryId).foreach(_ shouldBe uid)
    query.startTime shouldBe startTime
    query.i2b2QueryText shouldBe i2b2Xml
    query.extraXml shouldBe extraXml
    query.queryResults.length shouldBe 1
    query.queryResults.head.isInstanceOf[SuccessResult] shouldBe true
    val queryResult = query.queryResults.head.asInstanceOf[SuccessResult]
    queryResult.status shouldBe "success"
    List(queryResult.resultId, queryResult.adapterId).foreach(_ shouldBe uid)
    queryResult.count shouldBe resultCount
    queryResult.noiseTerms shouldBe noiseTerms
    queryResult.i2b2Mapping shouldBe i2b2Xml
    queryResult.flags shouldBe flags
    queryResult.breakdowns.length shouldBe 1
    queryResult.breakdowns.head shouldBe breakdown
    SuccessResult(queryResult.json.copy(_.count = 40)).get.count shouldBe 40
  }
}
