package com.gu.kindlegen

import com.gu.contentapi.client.model.v1.CapiDateTime
import org.joda.time.DateTime
import org.joda.time.format.{ DateTimeFormat, ISODateTimeFormat }

case class SectionHeading(
    title: String,
    titleLink: String
) {
  def toSectionString: String = {
    s"""<item>
       | <title>$title</title>
       | <link>$titleLink</link>
       |</item>
       |""".stripMargin
  }
}

case class SectionManifest(
    publicationDate: CapiDateTime,
    buildDate: DateTime,
    sections: Seq[SectionHeading]
) {
  val formattedPublicationDate = formatter.print(isoFormatter.parseDateTime(capiIsoDateTimeToString(publicationDate)))
  val formattedBuildDate = dtFormatter.print(buildDate)
  // TODO: filter for unique sections
  // TODO: Sort by pagenum to order
  val sectionsString = sections.map(_.toSectionString).mkString("")
  def toManifestContentsPage: String = {
    s"""
       |<?xml version="1.0" encoding="UTF-8" ?>
       |<rss version="2.0">
       |<channel>
       |<title>The Guardian / The Observer</title>
       |<link>http://www.guardian.co.uk/</link>
       |<pubDate>$formattedPublicationDate</pubDate>
       |<lastBuildDate>$formattedBuildDate</lastBuildDate>
       |$sectionsString</channel>
       |</rss>
      """.stripMargin
  }
  // formatter for use with parseDateTime to convert an isoDateTime: String to isoDateTime: DateTime
  private def isoFormatter = ISODateTimeFormat.dateTime()
  // formatter for use with parseDateTime or print to convert a DateTime: Long to DateTime: DateTime
  private def formatter = DateTimeFormat.forPattern("yyyyMMdd")
  private def capiIsoDateTimeToString(dt: CapiDateTime): String = dt.iso8601
  // formatter for use with parseDateTime or print to convert a DateTime: Long to DateTime: DateTime
  // TODO: in the NITF outputs the section manifest content page has a `Z` appended to the date. This is probably a mistake in the fingerpost script but worth checking
  private def dtFormatter = DateTimeFormat.forPattern("yyyyMMddHHmmss")
}
