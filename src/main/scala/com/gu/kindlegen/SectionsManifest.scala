package com.gu.kindlegen

import java.time.Instant

import com.gu.contentapi.client.model.v1.CapiDateTime
import com.gu.kindlegen.DateUtils._

case class SectionsManifest(
    publicationDate: CapiDateTime,
    buildDate: Instant,
    sections: Seq[SectionHeading]
) {
  val formattedPublicationDate: String = formatDate(publicationDate)
  val formattedBuildDate: String = dtFormatter.format(buildDate)
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

object SectionsManifest {
  def apply(sections: Seq[BookSection], buildDate: Instant = Instant.now): SectionsManifest = {
    SectionsManifest(
      publicationDate = sections.head.publicationDate,
      buildDate = buildDate,
      sections = sections.map(SectionHeading.apply)
    )
  }

  // TODO: Write to files/folders structure
}

case class SectionHeading(title: String, fileName: String) {
  def toSectionString: String = {
    s"""<item>
       | <title>$title</title>
       | <link>$fileName</link>
       |</item>
       |""".stripMargin
  }
}

object SectionHeading {
  def apply(section: BookSection): SectionHeading = SectionHeading(
    title = section.title,
    fileName = section.fileName
  )
}

