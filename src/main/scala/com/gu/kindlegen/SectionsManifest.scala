package com.gu.kindlegen

import java.time.Instant


object SectionsManifest {
  def apply(title: String, link: Link, books: Seq[BookSection], buildInstant: Instant = Instant.now): RssManifest = {
    RssManifest(
      title = title,
      link = link,
      buildInstant = buildInstant,
      publicationDate = books.head.publicationDate,
      items = books.map(_.section).map(section => RssItem(section.title, section.link))
    )
  }
}
