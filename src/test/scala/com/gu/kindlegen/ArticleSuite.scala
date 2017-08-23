package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.github.nscala_time.time.Imports._
import com.gu.contentapi.client.utils._

@RunWith(classOf[JUnitRunner])
class ArticleSuite extends FunSuite {

  test("Article.toNitf") {
    val expectedOutput = """
    |<?xml version="1.0" encoding="UTF-8"?>
    |<nitf version="-//IPTC//DTD NITF 3.3//EN">
    |<head>
    |<title>my title</title>
    |<docdata management-status="usable">
    |<doc-id id-string="section/date/title" />
    |<urgency ed-urg="2" />
    |<date.issue norm="20170724" />
    |<date.release norm="20170724" />
    |<doc.copyright holder="guardian.co.uk" />
    |</docdata>
    |<pubdata type="print" date.publication="20170724" />
    |</head>
    |<body>
    |<body.head>
    |<hedline><hl1>my title</hl1></hedline>
    |<byline>my name</byline>
    |<abstract>article abstract</abstract>
    |</body.head>
    |<body.content>content</body.content>
    |<body.end />
    |</body>
    |</nitf>""".stripMargin

    val article = Article(
      newspaperBookSection = "theguardian/mainsection/international",
      sectionName = "International",
      newspaperPageNumber = 2,
      title = "my title",
      docId = "section/date/title",
      issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
      releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
      pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
      byline = "my name",
      articleAbstract = "article abstract",
      content = "content"

    )
    assert(article.toNitf === expectedOutput)
  }

  private def formatter = DateTimeFormat.forPattern("yyyyMMdd")
}
