package com.gu.kindlegen

import org.scalatest.FlatSpec
import org.scalatest.Inspectors._
import org.scalatest.Matchers._

import com.gu.kindlegen.TestContent._


class BookSectionSuite extends FlatSpec {
  private def article(sectionId: String, sectionName: String, pageNum: Int) =
    Article(section = section(sectionId, sectionName), newspaperPageNumber = pageNum,
      "", "", ExampleLink, ExampleOffsetDate, "", "", Nil, None)

  private def section(sectionId: String, sectionName: String) =
    Section(id = sectionId, title = sectionName, link = ExampleLink)

  private val articlesInZ = (1 to 4).map(pageNum => article("z", "Z", pageNum))
  private val articlesInX = (1 to 4).map(pageNum => article("x", "X", pageNum))
  private val articlesInA = (4 to 5).map(pageNum => article("a", "A", pageNum))
  private val allArticles = articlesInA ++ articlesInX ++ articlesInZ

  ".fromArticles" should "return list of BookSections grouped by bookSectionId sorted by page number" in {
    val expected = List(  // sorted by page number then by section id
      BookSection(section("x", "X"), articlesInX),
      BookSection(section("z", "Z"), articlesInZ),
      BookSection(section("a", "A"), articlesInA)
    )

    BookSection.group(allArticles) should contain theSameElementsInOrderAs expected
  }

  it should "return empty list when given an empty list" in {
    BookSection.group(Nil) shouldBe empty
  }

  it should "filter out duplicate sections" in {
    val sections = BookSection.group(allArticles)
    sections.map(_.id) should contain theSameElementsAs sections.map(_.id).distinct
    sections.map(_.title) should contain theSameElementsAs sections.map(_.title).distinct
  }

  it should "group articles uniquely into sections" in {
    val sections = BookSection.group(allArticles)
    forAll(sections) { section =>
      val otherSections = sections.filterNot(_ eq section)
      val otherSectionArticles = otherSections.flatMap(_.articles)
      forAll(section.articles) { article =>
        otherSectionArticles should not contain article
      }
    }
  }
}
