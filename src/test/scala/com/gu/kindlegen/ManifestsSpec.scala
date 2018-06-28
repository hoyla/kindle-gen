package com.gu.kindlegen

import java.time.Instant

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

import com.gu.io.TempFiles
import com.gu.io.Link.RelativePath
import com.gu.kindlegen.TestData._

class ManifestsSpec extends FlatSpec with TempFiles {

  private val international = section("International", "theguardian/mainsection/international")
  private val topStories = section("Top Stories", "theguardian/mainsection/topstories")
  private val nonXmlChars = section("Non<XML> &chars;", "non/xml/chars")
  private val sections = Seq(international, topStories, nonXmlChars)

  private lazy val bookSections = MainSectionsBookBinder.default.group(articles)
  private val article = Article(section = null, 1, "", "", ExampleLink, ExampleOffsetDate, "", "", Nil, None)
  private val articles = Seq(
      (international, 1),
      (international, 2),
      (international, 2),
      (international, 3),
      (topStories   , 4),
      (topStories   , 4),
      (nonXmlChars  , 5)
    ).map { case (section, page) =>
        Article(section = section, newspaperPageNumber = page,
          "", "", ExampleLink, ExampleOffsetDate, "", "", Nil, None)
    }

  private def section(title: String, id: String) = {
    Section(id = id, title = title,
      link = RelativePath.from(id.replace('/', '_') + ".xml", null))
  }

  "SectionManifest" should "convert a sequence of bookSections to a section Manifest (Contents page)" in {
    val time = Instant.now()
    val manifest = SectionsManifest("", ExampleLink, bookSections, time)
    manifest.title shouldBe ""
    manifest.link shouldBe ExampleLink
    manifest.publicationDate shouldBe ExampleOffsetDate.toLocalDate
    manifest.buildInstant shouldBe time

    manifest.items should have length bookSections.length
    manifest.items.map(_.title) should contain theSameElementsInOrderAs sections.map(_.title)
    manifest.items.map(_.link.source) should contain theSameElementsInOrderAs sections.map(_.link.source)
  }
}
