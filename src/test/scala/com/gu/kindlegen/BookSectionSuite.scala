package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.github.nscala_time.time.Imports._
import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.client.utils._

@RunWith(classOf[JUnitRunner])
class BookSectionSuite extends FunSuite {

  def formatter = DateTimeFormat.forPattern("yyyyMMdd")
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
  println(s"articles = ${articles}")
  println(s"chunked articles = ${chunkedArticles}")
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
