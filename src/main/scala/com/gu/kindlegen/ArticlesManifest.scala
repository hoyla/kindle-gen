package com.gu.kindlegen

import java.time.Instant


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
