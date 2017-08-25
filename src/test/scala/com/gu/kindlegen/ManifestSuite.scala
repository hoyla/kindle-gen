package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.amazonaws.services.lambda.runtime.Context
import com.github.nscala_time.time.Imports._
import com.gu.contentapi.client.model.v1.CapiDateTime
import com.gu.contentapi.client.utils._

@RunWith(classOf[JUnitRunner])
class SectionManifestSuite extends FunSuite {
  import SectionManifest._

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
    assert(manifest.toManifestContentsPage === expectedOutput)
  }
  private def formatter = DateTimeFormat.forPattern("yyyyMMdd")
  private def dtFormatter = DateTimeFormat.forPattern("yyyyMMddHHmmss")

}
