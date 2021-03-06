package com.gu.kindlegen

import org.scalatest.FunSpec
import org.scalatest.Inspectors._
import org.scalatest.Matchers._

import com.gu.kindlegen.TestData._


class MainSectionsBookBinderSpec extends FunSpec {

  describe(".group") {
    it("works with an empty input list") {
      basicBinder.group(Seq.empty) shouldBe empty
    }

    it("works with no main sections") {
      val expected = List(  // sorted by page number then by section id
        BookSection(section("x"), articlesInX),
        BookSection(section("z"), articlesInZ),
        BookSection(section("a"), articlesInA)
      )

      basicBinder.group(allArticles) should contain theSameElementsInOrderAs expected
    }

    it("groups articles uniquely into sections") {
      val sections = basicBinder.group(allArticles)

      forAll(sections) { section =>
        val otherSections = sections.filterNot(_ eq section)
        val otherSectionArticles = otherSections.flatMap(_.articles)
        forAll(section.articles) { article =>
          otherSectionArticles should not contain article
        }
      }
    }

    it("combines sections") {
      val mainSection = MainSectionTemplate("azx", overrides = Seq("a", "z", "x"))
      val binder = new MainSectionsBookBinder(Seq(mainSection))

      val books = binder.group(allArticles)
      books should have size 1

      val articles = books.flatMap(_.articles)
      articles should contain theSameElementsAs allArticles
      articles.map(_.pageNumber) shouldBe sorted
    }

    it("sorts sections according to configuration") {
      val mainSections = Seq("z", "x", "a").map(MainSectionTemplate(_))
      val expectedArticleGroups = Seq(articlesInZ, articlesInX, articlesInA)

      val binder = new MainSectionsBookBinder(mainSections)
      val books = binder.group(allArticles)
      books should have size mainSections.size

      forEvery(books.zip(expectedArticleGroups)) { case (book, expectedArticles) =>
        book.articles.map(_.id) should contain theSameElementsAs expectedArticles.map(_.id)
      }
    }

    it("pushes unknown sections to the end") {
      val mainSection = MainSectionTemplate("a")
      val binder = new MainSectionsBookBinder(Seq(mainSection))
      val books = binder.group(allArticles)
      books.map(_.section.id) should contain theSameElementsInOrderAs Seq("a", "x", "z")
    }
  }

  private val basicBinder = new MainSectionsBookBinder(Seq.empty)

  private val allSections = "axz".map(id => section(id.toString))
  private val Seq(sectionA, sectionX, sectionZ) = allSections

  private val articlesInX = (1 to 5 by 2).map(pageNum => article(sectionX, pageNum))
  private val articlesInZ = (1 to 5     ).map(pageNum => article(sectionZ, pageNum))
  private val articlesInA = (4 to 5     ).map(pageNum => article(sectionA, pageNum))
  private val allArticles = articlesInA ++ articlesInX ++ articlesInZ

  private def article(section: Section, pageNum: Int) =
    TestData.article(id = s"${section.id}${pageNum.toString}", section = section, pageNum = pageNum)

  private def section(sectionId: String) =
    Section(id = sectionId, title = sectionId.toUpperCase, link = ExampleLink)


}
