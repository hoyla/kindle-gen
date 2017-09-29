package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.gu.contentapi.client.utils._
import DateUtils._

@RunWith(classOf[JUnitRunner])
class TestContentSuite extends FunSuite {

  val capiDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime
  val ta = TestContent("", "", 1, "", "", capiDate, capiDate, capiDate, "", "", "")

  test("TestContent.toContent") {
    assert(ta.toContent.id === "")
  }

  test("TestContent as factory") {
    assert(ta.toContent.fields.flatMap(_.headline) === Some(""))
    val ta2 = ta.copy(testArticleTitle = "new title")
    assert(ta2.toContent.fields.get.headline === Some("new title"))
    // add default args
  }

}
