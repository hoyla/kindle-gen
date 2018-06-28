package com.gu.kindlegen

import java.time.{OffsetDateTime, ZoneOffset}

import com.gu.io.Link

object TestData {
  val ExampleDate: OffsetDateTime = OffsetDateTime.of(2017, 7, 24, 0, 0, 0, 0, ZoneOffset.UTC)
  val ExampleLink = Link.AbsoluteURL.from("https://www.example.com")
  val ExampleSection = Section("a-section", "A Section", ExampleLink)
  val ExampleImage = Image("an-image", ExampleLink, Some("alt"), Some("caption"), Some("credit"))
  val ExampleArticle = article()

  def article(id: String = "a-section/an-article",
              title: String = "An Article",
              section: Section = ExampleSection,
              pageNum: Int = 0,
              link: Link = ExampleLink,
              pubDate: OffsetDateTime = ExampleDate,
              byline: String = "By me",
              articleAbstract: String = "Nothing special",
              bodyBlocks: Seq[String] = Seq("some", "text"),
              mainImage: Option[Image] = Some(ExampleImage)) =
    Article(id, title, section, pageNum, link, pubDate, byline, articleAbstract, bodyBlocks, mainImage)
}
