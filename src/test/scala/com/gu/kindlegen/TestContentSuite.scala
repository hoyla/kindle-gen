package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.gu.contentapi.client.model.v1.CapiDateTime
import com.gu.contentapi.client.model.v1._
import com.gu.contentapi.client.model.v1.ContentType
import com.gu.contentapi.client.model.v1.TagType.NewspaperBookSection
import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.client.model.v1.ContentFields
import com.github.nscala_time.time.Imports._
import com.gu.contentapi.client.utils._

@RunWith(classOf[JUnitRunner])
class TestContentSuite extends FunSuite {

  private def formatter = DateTimeFormat.forPattern("yyyyMMdd")

  test("TestContent.toContent") {
    val ta = TestContent("", "", 1, "", "", CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime, "", "", "")
    assert(ta.toContent.id === "")
  }

  test("TestContent as factory") {
    val ta = TestContent("", "", 1, "", "", CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime, "", "", "")
    assert(ta.toContent.fields.flatMap(_.headline) === Some(""))
    val ta2 = ta.copy(testArticleTitle = "new title")
    assert(ta2.toContent.fields.get.headline === Some("new title"))
    // add default args
  }

}
