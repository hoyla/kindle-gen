package com.gu.kindlegen

//import com.github.nscala_time.time.Imports._
import com.gu.contentapi.client.model.v1._
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

case class Article(
    newspaperBookSection: String,
    sectionName: String,
    title: String,
    docId: String,
    issueDate: CapiDateTime,
    // newspaperEditionDate in ContentFields
    releaseDate: CapiDateTime,
    pubDate: CapiDateTime,
    byline: String,
    articleAbstract: String, // standfirst is used
    content: String
) {
  def toNitf: String = {
    val formattedIssueDate = formatter.print(isoFormatter.parseDateTime(capiIsoDateTimeToString(issueDate)))
    val formattedReleaseDate = formatter.print(isoFormatter.parseDateTime(capiIsoDateTimeToString(releaseDate)))
    val formattedPubDate = formatter.print(isoFormatter.parseDateTime(capiIsoDateTimeToString(pubDate)))
    s"""
       |<?xml version="1.0" encoding="UTF-8"?>
       |<nitf version="-//IPTC//DTD NITF 3.3//EN">
       |<head>
       |<title>$title</title>
       |<docdata management-status="usable">
       |<doc-id id-string="$docId" />
       |<urgency ed-urg="2" />
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

  // formatter for use with parseDateTime to convert an isoDateTime: String to isoDateTime: DateTime
  private def isoFormatter = ISODateTimeFormat.dateTime()
  // formatter for use with parseDateTime or print to convert a DateTime: Long to DateTime: DateTime
  private def formatter = DateTimeFormat.forPattern("yyyyMMdd")
  private def capiIsoDateTimeToString(dt: CapiDateTime): String = dt.iso8601

}
