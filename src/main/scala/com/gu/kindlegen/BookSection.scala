package com.gu.kindlegen

import java.time.LocalDate

import scala.collection.breakOut

import com.gu.io.{Link, Linkable}



object BookSection extends BookBinder {
  val ordering = Ordering.by((section: BookSection) =>
    (section.firstPageNumber, section.lastPageNumber, section.title))

  def apply(section: Section, articles: Seq[Article]): BookSection = {
    new BookSection(section, articles.sortBy(_.newspaperPageNumber))
  }

  /** Groups articles into book sections, sorted according to each section's articles' page number */
  override def group(articles: Seq[Article]): Seq[BookSection] = {
    articles.groupBy(_.section)
      .map { case (section, articles) => apply(section, articles) }(breakOut)
      .sorted(ordering)
  }
}

case class BookSection private(section: Section, articles: Seq[Article]) extends Linkable {
  private val pageNumbers: Seq[Int] = articles.map(_.newspaperPageNumber)
  val firstPageNumber: Int = pageNumbers.min
  val lastPageNumber: Int = pageNumbers.max

  def id: String = section.id
  def title: String = section.title
  def link: Link = section.link
  lazy val publicationDate: LocalDate = articles.map(_.pubDate).min.toLocalDate

  def withLink(newLink: Link): BookSection = copy(section = section.copy(link = newLink))
}
