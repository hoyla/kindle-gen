package com.gu.kindlegen

import java.time.LocalDate

import com.gu.io.{Link, Linkable}


object BookSection {
  val ordering = Ordering.by((section: BookSection) =>
    (section.firstPageNumber, section.lastPageNumber, section.title))

  def apply(section: Section, articles: Seq[Article]): BookSection = {
    new BookSection(section, articles.sortBy(_.newspaperPageNumber))
  }
}

case class BookSection private(section: Section, articles: Seq[Article]) extends Linkable {
  private lazy val pageNumbers: Seq[Int] = articles.map(_.newspaperPageNumber)
  lazy val firstPageNumber: Int = pageNumbers.min
  lazy val lastPageNumber: Int = pageNumbers.max

  def id: String = section.id
  def title: String = section.title
  def link: Link = section.link
  lazy val publicationDate: LocalDate = articles.map(_.pubDate).min.toLocalDate

  def withLink(newLink: Link): BookSection = copy(section = section.copy(link = newLink))
}
