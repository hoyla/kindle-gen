package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import DateUtils._

@RunWith(classOf[JUnitRunner])
class ArticleNITFSuite extends FunSuite {

  test("ArticleNITF apply") {
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
      content = "content",
      imageUrl = None,
      fileId = 0
    )

    val expectedOutput =
      """
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
    val aNitf = ArticleNITF(article)
    assert(aNitf.fileContents === expectedOutput)
  }
}
