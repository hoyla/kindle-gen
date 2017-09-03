package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.gu.contentapi.client.utils._
import org.scalatest.FlatSpec
import DateUtils._
import org.joda.time.DateTime

class SectionManifestSpec extends FlatSpec {

  val capiDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime
  val ta = TestContent("", "", 1, "", "", capiDate, capiDate, capiDate, "", "", "", None)
  val tac = ta.toContent
  val taca = Article(tac)
  val article = Article(
    newspaperBookSection = "theguardian/mainsection/international",
    sectionName = "International",
    0, "my title", "", capiDate, capiDate, capiDate, "my name", "article abstract", "content", Some("image.URL")
  )
  val articles = List(article)
  val time = DateTime.now()

  "SectionManifest.apply" should "convert a sequence of articles to a section Manifest (Contents page)" in {

    assert(SectionManifest(articles, time) === SectionManifest(
      publicationDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
      buildDate = time,
      sections = List(SectionHeading("International", "theguardian/mainsection/international.xml"))
    ))
  }

  it should "use default datetime `now` if buildDate not passed" in {
    // FIXME: This test sometimes fails if, say, GC happens and causes a delay between evaluation of the test and the variable (DateTime.now)!!
    //    val time2 = DateTime.now()
    val sectionManifest = SectionManifest(articles)
    assert(sectionManifest.sections === List(SectionHeading("International", "theguardian/mainsection/international.xml")))

    //  case (l, m, n) => taca.copy(newspaperBookSection = m, sectionName = l, newspaperPageNumber = n )
  }

  @RunWith(classOf[JUnitRunner])
  class SectionManifestSuite extends FunSuite {

    test("SectionManifest.toManifestContentsPage") {
      val time = dtFormatter.parseDateTime("20170519011102")
      val manifest = SectionManifest(
        publicationDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        buildDate = time,
        sections = List(
          SectionHeading(title = "title1", titleLink = "link1"),
          SectionHeading(title = "title2", titleLink = "link2")
        )
      )
      val expectedOutput =
        s"""
         |<?xml version="1.0" encoding="UTF-8" ?>
         |<rss version="2.0">
         |<channel>
         |<title>The Guardian / The Observer</title>
         |<link>http://www.guardian.co.uk/</link>
         |<pubDate>20170724</pubDate>
         |<lastBuildDate>20170519011102</lastBuildDate>
         |<item>
         | <title>title1</title>
         | <link>link1</link>
         |</item>
         |<item>
         | <title>title2</title>
         | <link>link2</link>
         |</item>
         |</channel>
         |</rss>
      """.stripMargin
      assert(manifest.
        toManifestContentsPage === expectedOutput)
    }

  }
}
