package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
@RunWith(classOf[JUnitRunner])
class LambdaSuite extends FunSuite {

  test("string take") {
    val message = "hello, world"
    assert(message.take(5) === "hello")
  }
}
