package com.gu.kindlegen

import org.scalatest.FlatSpec
import org.scalatest.Inspectors._
import org.scalatest.Matchers._


class BookSectionSuite extends FlatSpec {
  private def article(sectionId: String, sectionName: String, pageNum: Int) =
    Article(sectionId = sectionId, sectionName = sectionName, newspaperPageNumber = pageNum,
      "", "", DateUtils.exampleDate, "", "", Nil, None)

  private val articlesInZ = (1 to 4).map(pageNum => article("z", "Z", pageNum))
  private val articlesInX = (1 to 4).map(pageNum => article("x", "X", pageNum))
  private val articlesInA = (4 to 5).map(pageNum => article("a", "A", pageNum))
  private val allArticles = articlesInA ++ articlesInX ++ articlesInZ

  ".fromArticles" should "return list of list of BookSectionPages grouped by bookSectionId" in {
    val expected = List(  // sorted by page number then by section id
      BookSection("x", "X", articlesInX),
      BookSection("z", "Z", articlesInZ),
      BookSection("a", "A", articlesInA)
    )

    BookSection.fromArticles(allArticles) should contain theSameElementsInOrderAs expected
  }

  it should "return empty list when given an empty list" in {
    BookSection.fromArticles(Nil) shouldBe empty
  }

  it should "filter out duplicate sections" in {
    val sections = BookSection.fromArticles(allArticles)
    sections.map(_.id) should contain theSameElementsAs sections.map(_.id).distinct
    sections.map(_.title) should contain theSameElementsAs sections.map(_.title).distinct
  }

  it should "group articles uniquely into sections" in {
    val sections = BookSection.fromArticles(allArticles)
    forAll(sections) { section =>
      val otherSections = sections.filterNot(_ eq section)
      val otherSectionArticles = otherSections.flatMap(_.articles)
      forAll(section.articles) { article =>
        otherSectionArticles should not contain article
      }
    }
  }
}
