package com.gu.kindlegen

import scala.collection.breakOut

/**
 * Each Book (eg Guardian or Observer) contains many sections (eg G2, Top Stories, Finance)
 */
case class BookSection(section: Section, articles: Seq[Article]) {
  private val pageNumbers: Seq[Int] = articles.map(_.newspaperPageNumber)
  val firstPageNumber: Int = pageNumbers.min
  val lastPageNumber: Int = pageNumbers.max

  def id: String = section.id
  def title: String = section.title

  def publicationDate = articles.map(_.pubDate).minBy(_.dateTime)
  def fileName: String = s"${id.replace('/', '_')}.xml"
}

object BookSection {
  /** Groups articles into book sections, sorted according to each section's articles' page number */
  def fromArticles(articles: Seq[Article]): Seq[BookSection] = {
    articles.groupBy(_.section)
      .values.map(apply)(breakOut)
      .sortBy(section => (section.firstPageNumber, section.lastPageNumber, section.title))
  }

  /** Creates a book section from a collection of articles that all belong to the same section */
  def apply(articles: Seq[Article]): BookSection = {
    require(articles.nonEmpty, "A book section must have at least one article!")

    val anArticle = articles.head
    val section = anArticle.section

    require(articles.forall(a => a.section == section),
      s"All articles must belong to the same section! Found ${articles.map(_.section).distinct}.")

    BookSection(section, articles)
  }
}
