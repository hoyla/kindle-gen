package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.gu.kindlegen.TestContent._

@RunWith(classOf[JUnitRunner])
class TestContentSuite extends FunSuite {

  val capiDate = ExampleDate
  val ta = TestContent("", "", 1, "", "", capiDate, capiDate, capiDate, "", "", "", None)

  test("TestContent.toContent") {
    assert(ta.toContent.id === "")
  }

  test("TestContent as factory") {
    assert(ta.toContent.fields.flatMap(_.headline) === Some(""))
    val ta2 = ta.copy(testArticleTitle = "new title")
    assert(ta2.toContent.fields.get.headline === Some("new title"))
  }

}
