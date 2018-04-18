package com.gu.kindlegen

import java.time.Instant

import org.scalatest.FlatSpec
import DateUtils._

class SectionManifestSpec extends FlatSpec {

  val capiDate = exampleDate
  val ta = Article(TestContent("", "", 1, "", "", capiDate, capiDate, capiDate, "", "", "", None, 0).toContent, 0)

  val articles = {
    Seq(
      ("International", "theguardian/mainsection/international", 1),
      ("International", "theguardian/mainsection/international", 2),
      ("International", "theguardian/mainsection/international", 2),
      ("International", "theguardian/mainsection/international", 3),
      ("Top Stories", "theguardian/mainsection/topstories", 4),
      ("Top Stories", "theguardian/mainsection/topstories", 4)
    ).map {

        case (l, m, n) => ta.copy(sectionName = l, sectionId = m, newspaperPageNumber = n)
      }
  }

  private val time = Instant.now()

  "SectionManifest.apply" should "convert a sequence of articles to a section Manifest (Contents page)" in {

    assert(SectionManifest(articles, time) === SectionManifest(
      publicationDate = exampleDate,
      buildDate = time,
      sections = List(SectionHeading("International", "theguardian/mainsection/international"), SectionHeading("Top Stories", "theguardian/mainsection/topstories"))
    ))
  }

  it should "use default datetime `now` if buildDate not passed" in {
    val sectionManifest = SectionManifest(articles)
    assert(sectionManifest.sections ===
      List(
        SectionHeading("International", "theguardian/mainsection/international"),
        SectionHeading("Top Stories", "theguardian/mainsection/topstories")
      ))

  }
  val sectionHeadingList = SectionManifest.toSectionHeading(articles)
  val manifest = SectionManifest(
    publicationDate = exampleDate,
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
         |<lastBuildDate>${dtFormatter.format(time)}</lastBuildDate>
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
        SectionHeading(
          "Top Stories",
          "theguardian/mainsection/topstories"
        )
      )
    assert(SectionManifest.toSectionHeading(articles) === expectedOutput)
  }

  ".toSectionString" should "add `.xml` and change slash to underscore in titleLink" in {
    val sectionHeading = SectionHeading(articles.head)

    val expectedOutput = {
      """<item>
        | <title>International</title>
        | <link>theguardian_mainsection_international.xml</link>
        |</item>
        |""".stripMargin
    }
    assert(sectionHeading.toSectionString === expectedOutput)
  }
}
