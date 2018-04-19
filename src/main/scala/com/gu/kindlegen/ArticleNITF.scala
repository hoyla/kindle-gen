package com.gu.kindlegen

import DateUtils._

case class ArticleNITF(fileContents: String)

object ArticleNITF {

  def apply(article: Article) = new ArticleNITF(
    fileContents = s"""
     |<?xml version="1.0" encoding="UTF-8"?>
     |<nitf version="-//IPTC//DTD NITF 3.3//EN">
     |<head>
     |<title>${article.title}</title>
     |<docdata management-status="usable">
     |<doc-id id-string="${article.docId}" />
     |<urgency ed-urg="2" />
     |<date.issue norm="${formatDate(article.pubDate)}" />
     |<date.release norm="${formatDate(article.pubDate)}" />
     |<doc.copyright holder="guardian.co.uk" />
     |</docdata>
     |<pubdata type="print" date.publication="${formatDate(article.pubDate)}" />
     |</head>
     |<body>
     |<body.head>
     |<hedline><hl1>${article.title}</hl1></hedline>
     |<byline>${article.byline}</byline>
     |<abstract>${article.articleAbstract}</abstract>
     |</body.head>
     |<body.content>${article.content}</body.content>
     |<body.end />
     |</body>
     |</nitf>""".stripMargin
  )
}
