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
  val ta = TestContent("", "", 1, "", "", capiDate, capiDate, capiDate, "", "", "")
  val article = Article(
    newspaperBookSection = "theguardian/mainsection/international",
    sectionName = "International",
    0, "my title", "", capiDate, capiDate, capiDate, "my name", "article abstract", "content"
  )
  val articles = List(article)
  val time = DateTime.now()

  "SectionManifest.apply" should "convert a sequence of articles to a section Manifest (Contents page)" in {

    assert(SectionManifest(articles, time) === SectionManifest(
      publicationDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
      buildDate = time,
      sections = List(SectionHeading("International", "theguardian/mainsection/international"))
    ))
  }

  it should "use default datetime `now` if buildDate not passed" in {
    val time2 = DateTime.now()
    val sectionManifest = SectionManifest(articles)
    assert(sectionManifest.sections === List(SectionHeading(
      "International",
      "theguardian/mainsection/international"
    )))
  }

}
@RunWith(classOf[JUnitRunner])
class SectionManifestSuite extends FlatSpec {

  val capiDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime
  val ta = Article(TestContent("", "", 1, "", "", capiDate, capiDate, capiDate, "", "", "").toContent)

  val articles = {
    Seq(
      ("International", "theguardian/mainsection/international", 1),
      ("International", "theguardian/mainsection/international", 2),
      ("International", "theguardian/mainsection/international", 2),
      ("International", "theguardian/mainsection/international", 3),
      ("Top Stories", "theguardian/mainsection/topstories", 4),
      ("Top Stories", "theguardian/mainsection/topstories", 4)
    ).map {

        case (l, m, n) => ta.copy(sectionName = l, newspaperBookSection = m, newspaperPageNumber = n)
      }
  }
  val time = dtFormatter.parseDateTime("20170519011102")
  val sectionHeadingList = SectionManifest.toSectionHeading(articles)
  val manifest = SectionManifest(
    publicationDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
    buildDate = time,
    sections = SectionManifest.toSectionHeading(articles)
  )

  ".toManifestContentsPage" should "create xml hierachical-title-manifest" in {
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
         | <title>International</title>
         | <link>theguardian_mainsection_international.xml</link>
         |</item>
         |<item>
         | <title>Top Stories</title>
         | <link>theguardian_mainsection_topstories.xml</link>
         |</item>
         |</channel>
         |</rss>
      """.stripMargin
    assert(manifest.toManifestContentsPage === expectedOutput)
  }

  ".toSectionHeading" should "filter out duplicate sections" in {
    val expectedOutput =
      List(
        SectionHeading("International", "theguardian/mainsection/international"),
        SectionHeading("Top Stories", "theguardian/mainsection/topstories")
      )
    assert(SectionManifest.toSectionHeading(articles) === expectedOutput)
  }

  ".toSectionString" should "add `.xml` and change slash to underscore in titleLink" in {
    val sectionHeading = SectionHeading(articles.head)
    val expectedOutput =
      """<item>
        | <title>International</title>
        | <link>theguardian_mainsection_international.xml</link>
        |</item>
        |""".stripMargin
    assert(sectionHeading.toSectionString === expectedOutput)
  }
}
