package com.gu.kindlegen

import com.gu.contentapi.client.model.v1.CapiDateTime
import org.joda.time.DateTime
import DateUtils._

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
  // TODO: in the NITF outputs the section manifest content page has a `Z` appended to the date. This is probably a mistake in the fingerpost script but worth checking
}

object SectionManifest {
  def apply(articles: Seq[Article], buildDate: DateTime = DateTime.now): SectionManifest = {
    new SectionManifest(
      publicationDate = articles.head.issueDate,
      buildDate = buildDate,
      sections = toSectionHeading(articles)
    )
  }

  // TODO: Write test
  def toSectionHeading(articles: Seq[Article]): Seq[SectionHeading] = {
    articles.map(x =>
      SectionHeading(
        title = x.sectionName,
        titleLink = x.newspaperBookSection + ".xml"
      ))
  }
}
