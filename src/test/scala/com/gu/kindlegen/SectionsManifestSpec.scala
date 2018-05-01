package com.gu.kindlegen

import java.time.{Instant, LocalDate}

import scala.xml.Utility

import org.scalatest.FlatSpec

import com.gu.kindlegen.DateUtils._
import com.gu.kindlegen.Link.RelativePath
import com.gu.kindlegen.TestContent._
import org.scalatest.Matchers._

class SectionsManifestSpec extends FlatSpec {

  private val capiDate = ExampleDate
  private val time = Instant.now()

  private val content = TestContent("", "", 1, "", "", capiDate, capiDate, capiDate, "", "", "", None).toContent
  private val article = Article(content, ExampleQuerySettings.sectionTagType)

  private val international = section("International", "theguardian/mainsection/international")
  private val topStories = section("Top Stories", "theguardian/mainsection/topstories")
  private val nonXmlChars = section("Non<XML> &chars;", "non/xml/chars")
  private val sections = Seq(international, topStories, nonXmlChars)

  private lazy val bookSections = BookSection.fromArticles(articles)
  private val articles = Seq(
      (international, 1),
      (international, 2),
      (international, 2),
      (international, 3),
      (topStories   , 4),
      (topStories   , 4),
      (nonXmlChars  , 5)
    ).map {
      case (section, page) => article.copy(newspaperPageNumber = page, section = section)
    }

  private def section(title: String, id: String) = {
    Section(id = id, title = title,
      link = RelativePath.from(id.replace('/', '_') + ".xml", null))
  }

  "SectionManifest.apply" should "convert a sequence of bookSections to a section Manifest (Contents page)" in {
    val manifest = SectionsManifest("", ExamplePath, bookSections, time)
    manifest.title shouldBe ""
    manifest.link shouldBe ExamplePath
    manifest.publicationDate shouldBe ExampleOffsetDate.toLocalDate
    manifest.buildInstant shouldBe time

    manifest.items should have length bookSections.length
    manifest.items.map(_.title) should contain theSameElementsInOrderAs sections.map(_.title)
    manifest.items.map(_.link.source) should contain theSameElementsInOrderAs sections.map(_.link.source)
  }
}
