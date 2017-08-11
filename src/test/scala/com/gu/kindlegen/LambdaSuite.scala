package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.amazonaws.services.lambda.runtime.Context
import com.gu.contentapi.client.model.v1.Content

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class LambdaSuite extends FunSuite {

  import Lambda._

  test("string take") {
    val message = "hello, world"
    assert(message.take(5) === "hello")
  }

  // TODO: Find a way to override the source file to a sample.conf version
  test("Querier - readApiKey") {
    //    override val configSource = Source.fromFile("sample.conf")
    assert(Querier.readApiKey !== "test")
  }

  // TODO: Find a way to test printSentResponse, extract the edition dates etc

  // TODO: Find a way to test resultToArticle

}

