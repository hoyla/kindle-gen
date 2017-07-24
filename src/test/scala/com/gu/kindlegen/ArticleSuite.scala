package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.amazonaws.services.lambda.runtime.Context

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
    |</nitf>""".stripMargin

    val article = Article(
      title = "my title",
      status = "usable",
      idString = "section/date/title",
      urgency = 3,
      issueDate = 20170724,
      releaseDate = 20170724,
      pubDate = 20170724
    )
    assert(article.toNitf === expectedOutput)
  }

}

