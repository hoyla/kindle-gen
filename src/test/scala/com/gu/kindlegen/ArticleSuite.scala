package com.gu.kindlegen

import org.scalatest.FlatSpec
import DateUtils._

class ArticleSuite extends FlatSpec {

  val capiDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime
  val ta = TestContent("", "", 2, "", "", capiDate, capiDate, capiDate, "", "", "", None, 0)

  ".apply" should "apply method with content as parameter" in {
    val content = ta.toContent
    val a = Article((content, 0))
    assert(a.newspaperPageNumber === 2)
  }
}
