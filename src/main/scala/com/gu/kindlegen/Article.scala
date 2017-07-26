package com.gu.kindlegen

import com.github.nscala_time.time.Imports._

case class Article(
    title: String,
    status: String,
    idString: String,
    urgency: Int, // TODO: Use an enum
    issueDate: DateTime,
    releaseDate: DateTime,
    pubDate: DateTime,
    byline: String,
    articleAbstract: String,
    content: String
) {
  def toNitf: String = {
    val formattedIssueDate = formatter.print(issueDate)
    val formattedReleaseDate = formatter.print(releaseDate)
    val formattedPubDate = formatter.print(pubDate)
    s"""
    |<?xml version="1.0" encoding="UTF-8"?>
    |<nitf version="-//IPTC//DTD NITF 3.3//EN">
    |<head>
    |<title>$title</title>
    |<docdata management-status="$status">
    |<doc-id id-string="$idString" />
    |<urgency ed-urg="$urgency" />
    |<date.issue norm="$formattedIssueDate" />
    |<date.release norm="$formattedReleaseDate" />
    |<doc.copyright holder="guardian.co.uk" />
    |</docdata>
    |<pubdata type="print" date.publication="$formattedPubDate" />  
    |</head>
    |<body>
    |<body.head>
    |<hedline><hl1>$title</hl1></hedline>
    |<byline>$byline</byline>
    |<abstract>$articleAbstract</abstract>
    |</body.head>
    |<body.content>$content</body.content>
    |<body.end />
    |</body>
    |</nitf>""".stripMargin
  }

  private def formatter = DateTimeFormat.forPattern("yyyyMMdd")

}
