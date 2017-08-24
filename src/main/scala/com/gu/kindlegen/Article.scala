package com.gu.kindlegen

//import com.github.nscala_time.time.Imports._
import com.gu.contentapi.client.model.v1.TagType.NewspaperBookSection
import com.gu.contentapi.client.model.v1._
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

case class Article(
    newspaperBookSection: String,
    sectionName: String,
    newspaperPageNumber: Int,
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

object Article {
  def apply(content: Content) = new Article(
    newspaperBookSection = content.tags.find(_.`type` == NewspaperBookSection).get.id, // FIXME: NB this will throw exception if this tag is missing!
    sectionName = content.tags.find(_.`type` == NewspaperBookSection).get.webTitle, // FIXME: NB this will throw exception if this tag is missing!
    newspaperPageNumber = content.fields.flatMap(_.newspaperPageNumber).getOrElse(0),
    title = content.fields.flatMap(_.headline).getOrElse("").toString,
    docId = content.id,
    issueDate = content.fields.flatMap(_.newspaperEditionDate).get,
    releaseDate = content.fields.flatMap(_.newspaperEditionDate).get,
    pubDate = content.fields.flatMap(_.newspaperEditionDate).get,
    byline = content.fields.flatMap(_.byline).getOrElse(""),
    articleAbstract = content.fields.flatMap(_.standfirst).getOrElse(""),
    content = content.fields.flatMap(_.body).getOrElse("")
  )
}