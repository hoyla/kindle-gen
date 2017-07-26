package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.amazonaws.services.lambda.runtime.Context
import com.github.nscala_time.time.Imports._

@RunWith(classOf[JUnitRunner])
class ArticleSuite extends FunSuite {
  import Article._

  test("Article.toNitf") {
    val expectedOutput = """
    |<?xml version="1.0" encoding="UTF-8"?>
    |<nitf version="-//IPTC//DTD NITF 3.3//EN">
    |<head>
    |<title>my title</title>
    |<docdata management-status="usable">
    |<doc-id id-string="section/date/title" />
    |<urgency ed-urg="3" />
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
      title = "my title",
      status = "usable",
      idString = "section/date/title",
      urgency = 3,
      issueDate = new DateTime("2017-07-24"),
      releaseDate = new DateTime("2017-07-24"),
      pubDate = new DateTime("2017-07-24"),
      byline = "my name",
      articleAbstract = "article abstract",
      content = "content"

    )
    assert(article.toNitf === expectedOutput)
  }

}
