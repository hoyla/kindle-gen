package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.client.utils._
import DateUtils._

@RunWith(classOf[JUnitRunner])
class ArticleSuite extends FunSuite {

  val capiDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime
  val ta = TestContent("", "", 2, "", "", capiDate, capiDate, capiDate, "", "", "")

  test("apply method with content as parameter") {
    val content = ta.toContent
    val a = Article(content: Content)
    assert(a.newspaperPageNumber === 2)
  }
}
