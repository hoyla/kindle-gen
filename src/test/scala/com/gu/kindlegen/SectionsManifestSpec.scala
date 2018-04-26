package com.gu.kindlegen

import java.time.Instant

import org.scalatest.FlatSpec

import com.gu.kindlegen.DateUtils._

class SectionsManifestSpec extends FlatSpec {

  val capiDate = exampleDate
  val ta = Article(TestContent("", "", 1, "", "", capiDate, capiDate, capiDate, "", "", "", None).toContent)

  val articles = {
    Seq(
      ("International", "theguardian/mainsection/international", 1),
      ("International", "theguardian/mainsection/international", 2),
      ("International", "theguardian/mainsection/international", 2),
      ("International", "theguardian/mainsection/international", 3),
      ("Top Stories", "theguardian/mainsection/topstories", 4),
      ("Top Stories", "theguardian/mainsection/topstories", 4)
    ).map {
        case (title, id, page) => ta.copy(newspaperPageNumber = page,
          section = Section(id = id, title = title, link = null))
      }
  }

  val sections = BookSection.fromArticles(articles)

  private val time = Instant.now()

  "SectionManifest.apply" should "convert a sequence of sections to a section Manifest (Contents page)" in {
    assert(SectionsManifest("", sections, time) === SectionsManifest(
      "",
      publicationDate = exampleDate,
      buildDate = time,
      sections = List(
        SectionHeading("International", fileName = "theguardian_mainsection_international.xml"),
        SectionHeading("Top Stories", fileName = "theguardian_mainsection_topstories.xml"))
    ))
  }

  ".toManifestContentsPage" should "create xml hierachical-title-manifest" in {
    val manifest = SectionsManifest("The Guardian / The Observer", sections, time)

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

  ".toSectionString" should "add `.xml` and change slash to underscore in titleLink" in {
    val sectionHeading = SectionHeading(BookSection(Seq(articles.head)))

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
