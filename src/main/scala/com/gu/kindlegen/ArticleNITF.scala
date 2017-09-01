package com.gu.kindlegen
import com.gu.contentapi.client.model.v1.CapiDateTime
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.format.DateTimeFormat

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
     |<date.issue norm="${formatter.print(isoFormatter.parseDateTime(capiIsoDateTimeToString(article.issueDate)))}" />
     |<date.release norm="${formatter.print(isoFormatter.parseDateTime(capiIsoDateTimeToString(article.releaseDate)))}" />
     |<doc.copyright holder="guardian.co.uk" />
     |</docdata>
     |<pubdata type="print" date.publication="${formatter.print(isoFormatter.parseDateTime(capiIsoDateTimeToString(article.releaseDate)))}" />
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
  // formatter for use with parseDateTime to convert an isoDateTime: String to isoDateTime: DateTime
  private def isoFormatter = ISODateTimeFormat.dateTime()
  // formatter for use with parseDateTime or print to convert a DateTime: Long to DateTime: DateTime
  private def formatter = DateTimeFormat.forPattern("yyyyMMdd")
  private def capiIsoDateTimeToString(dt: CapiDateTime): String = dt.iso8601
}
