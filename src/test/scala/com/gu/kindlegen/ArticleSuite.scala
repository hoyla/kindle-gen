package com.gu.kindlegen

import org.scalatest.FlatSpec
import DateUtils._

class ArticleSuite extends FlatSpec {

  val capiDate = exampleDate
  val ta = TestContent("", "", 2, "", "", capiDate, capiDate, capiDate, "", "", "", None)

  ".apply" should "apply method with content as parameter" in {
    val content = ta.toContent
    val a = Article(content)
    assert(a.newspaperPageNumber === 2)
  }
}
