package com.gu.kindlegen

import java.time.Instant

import com.gu.io.Link


object ArticlesManifest {
  def apply(section: BookSection, buildInstant: Instant = Instant.now): RssManifest = {
    RssManifest(
      title = section.title,
      link = section.link,
      buildInstant = buildInstant,
      publicationDate = section.publicationDate,
      items = section.articles.map(article => RssItem(article.title, article.link))
    )
  }
}

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
