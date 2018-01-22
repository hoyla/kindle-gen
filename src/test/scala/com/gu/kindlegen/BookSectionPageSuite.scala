package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import DateUtils._

@RunWith(classOf[JUnitRunner])
class BookSectionPageSuite extends FunSuite {

  val capiDate = formatter.parseDateTime("20170724").toCapiDateTime
  val ta = Article((TestContent("", "", 1, "", "", capiDate, capiDate, capiDate, "", "", "", None, 0).toContent), 0)

  val articles: List[Article] = List(1, 1, 2, 2, 3).map(n => ta.copy(newspaperPageNumber = n))

  val chunkedArticles = BookSectionPage.chunkByPageNum(articles)

  test("BookSectionPage.chunkByPageNum sorts article into lists of lists by pagenum") {
    assert(BookSectionPage.chunkByPageNum(articles).map(_.map(_.newspaperPageNumber)) === List(List(1, 1), List(2, 2), List(3)))
  }

  test("BookSectionPage.chunksToBSP converts list of list of articles to list of BSPs") {
    assert(
      BookSectionPage.chunksToBSP(chunkedArticles).map(bsp => {
        (s"book section page = ${bsp.pageNum}, number of articles on page = ${bsp.articles.length}")
      }) === (List(
        ("book section page = 1, number of articles on page = 2"),
        ("book section page = 2, number of articles on page = 2"),
        ("book section page = 3, number of articles on page = 1")
      ))
    )
  }
}
