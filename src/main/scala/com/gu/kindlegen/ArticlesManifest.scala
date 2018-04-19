package com.gu.kindlegen

import java.time.Instant

import com.gu.contentapi.client.model.v1.CapiDateTime
import DateUtils._

// This will be called/used with a subsection of articles that have been chunked by section.
// OR it will be the first thing to be called/applied...
case class ArticlesManifest(
    title: String,
    publicationDate: CapiDateTime,
    buildDate: Instant,
    articles: Seq[ArticleHeading]
) {
  val formattedPublicationDate: String = formatDate(publicationDate)
  val formattedBuildDate: String = dtFormatter.format(buildDate)
  val sectionsString: String = articles.map(_.toArticleHeadingString).mkString("")
  def toSubsectionContentsPage: String = {
    s"""
       |<?xml version="1.0" encoding="UTF-8" ?>
       |<rss version="2.0">
       |<channel>
       |<title>$title</title>
       |<pubDate>$formattedPublicationDate</pubDate>
       |<lastBuildDate>$formattedBuildDate</lastBuildDate>
       |$sectionsString</channel>
       |</rss>
      """.stripMargin
  }
}

object ArticlesManifest {
  def apply(articles: Seq[Article], buildDate: Instant = Instant.now): ArticlesManifest = {
    ArticlesManifest(
      // this is the chunk we are creating the contents for
      title = articles.head.sectionId,
      publicationDate = articles.head.pubDate,
      buildDate = buildDate,
      articles = toArticleHeading(articles)
    )
  }

  // takes a Seq[Article] that are already chunked
  def toArticleHeading(articles: Seq[Article]): Seq[ArticleHeading] = {
    articles.map(article => ArticleHeading(article))
  }
}

case class ArticleHeading(
    title: String,
    titleLink: String
) {
  def toArticleHeadingString: String = {
    val convertedTitleLink = titleLink.replace("/", "_")
    s"""<item>
       | <title>$title</title>
       | <link>$convertedTitleLink.xml</link>
       |</item>
       |""".stripMargin
  }
}

object ArticleHeading {
  def apply(article: Article): ArticleHeading = ArticleHeading(
    title = article.title,
    titleLink = article.fileName
  )
}
