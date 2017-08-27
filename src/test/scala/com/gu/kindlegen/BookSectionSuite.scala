package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.gu.contentapi.client.utils._
import DateUtils._

@RunWith(classOf[JUnitRunner])
class BookSectionSuite extends FunSuite {

  val capiDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime
  val ta = Article(TestContent("", "", 1, "", "", capiDate, capiDate, capiDate, "", "", "").toContent)

  val articles = {
    List(
      ("theguardian/mainsection/international", 1),
      ("theguardian/mainsection/international", 2),
      ("theguardian/mainsection/international", 2),
      ("theguardian/mainsection/international", 3),
      ("theguardian/mainsection/topstories", 4),
      ("theguardian/mainsection/topstories", 4)
    ).map {

        case (m, n) => ta.copy(newspaperBookSection = m, newspaperPageNumber = n)
      }
  }

  val chunkedArticles = BookSectionPage.chunkByPageNum(articles)
  val BSPs = BookSectionPage.chunksToBSP(chunkedArticles)

  test("chunkBookSectionPagesByBookSection") {
    assert(BookSection.chunkBookSectionPages(BSPs).map(_.map(_.bookSectionId)) ===
      List(
        List(
          "theguardian/mainsection/international",
          "theguardian/mainsection/international",
          "theguardian/mainsection/international"
        ),
        List(
          "theguardian/mainsection/topstories"
        )
      ))
  }
  //      s"BookSection ${bs.bookSectionId} contains pages ${bs.}

}
