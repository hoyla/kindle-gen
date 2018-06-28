package com.gu.kindlegen

import java.time.{LocalDate, OffsetDateTime}

import com.gu.io.{Link, Linkable}


case class Article(id: String,
                   title: String,
                   section: Section,
                   pageNumber: Int,
                   link: Link,
                   pubDate: OffsetDateTime,
                   byline: String,
                   articleAbstract: String,
                   bodyBlocks: Seq[String],
                   mainImage: Option[Image]) extends Linkable


case class Image(id: String,
                 link: Link,
                 altText: Option[String],
                 caption: Option[String],
                 credit: Option[String]) extends Linkable


case class ImageData(metadata: Image, data: Array[Byte]) {
  def source: String = metadata.link.source
}


case class Section(id: String, title: String, link: Link)


object BookSection {
  val ordering = Ordering.by((section: BookSection) =>
    (section.firstPageNumber, section.lastPageNumber, section.title))

  def apply(section: Section, articles: Seq[Article]): BookSection = {
    new BookSection(section, articles.sortBy(_.pageNumber))
  }
}

case class BookSection private(section: Section, articles: Seq[Article]) extends Linkable {
  private lazy val pageNumbers: Seq[Int] = articles.map(_.pageNumber)
  lazy val firstPageNumber: Int = pageNumbers.min
  lazy val lastPageNumber: Int = pageNumbers.max

  def id: String = section.id
  def title: String = section.title
  def link: Link = section.link
  lazy val publicationDate: LocalDate = articles.map(_.pubDate).min.toLocalDate

  def withLink(newLink: Link): BookSection = copy(section = section.copy(link = newLink))
}
