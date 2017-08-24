package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.github.nscala_time.time.Imports._
import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.client.utils._

@RunWith(classOf[JUnitRunner])
class ArticleSuite extends FunSuite {

  private def formatter = DateTimeFormat.forPattern("yyyyMMdd")

  test("apply method with content as parameter") {
    val ta = TestArticle("", "", 2, "", "", CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime, "", "", "")
    val content = ta.toContent
    val a = Article(content: Content)
    assert(a.newspaperPageNumber === 2)
  }
}
