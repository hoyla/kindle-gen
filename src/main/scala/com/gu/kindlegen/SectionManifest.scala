package com.gu.kindlegen

import com.gu.contentapi.client.model.v1.CapiDateTime
import org.joda.time.DateTime
import DateUtils._

case class SectionManifest(
    publicationDate: CapiDateTime,
    buildDate: DateTime,
    sections: Seq[SectionHeading]
) {
  val formattedPublicationDate: String = isoDateConverter(publicationDate)
  val formattedBuildDate: String = dtFormatter.print(buildDate)
  // TODO: filter for unique sections
  // TODO: Sort by pagenum to order
  val sectionsString: String = sections.map(_.toSectionString).mkString("")
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
  // TODO: in the NITF outputs the section manifest content page has a `Z` appended to the date. This is probably a mistake in the fingerpost script but worth checking ASK DB
}

object SectionManifest {
  def apply(articles: Seq[Article], buildDate: DateTime = DateTime.now): SectionManifest = {
    SectionManifest(
      publicationDate = articles.head.issueDate,
      buildDate = buildDate,
      sections = toSectionHeading(articles)
    )
  }

  // TODO: Write test
  // TODO: Filter duplicates
  // TODO: Replace / with _
  def toSectionHeading(articles: Seq[Article]): Seq[SectionHeading] = {
    val allHeadings = articles.map(article =>
      SectionHeading(article))
    allHeadings.distinct
  }

  // TODO: Write to files/folders
}

case class SectionHeading(
    title: String,
    titleLink: String
) {
  def toSectionString: String = {
    val convertedTitleLink = titleLink.replace("/", "_")
    s"""<item>
       | <title>$title</title>
       | <link>$convertedTitleLink.xml</link>
       |</item>
       |""".stripMargin
  }
}

object SectionHeading {
  def apply(article: Article): SectionHeading = SectionHeading(
    title = article.sectionName,
    titleLink = article.newspaperBookSection
  )
}

