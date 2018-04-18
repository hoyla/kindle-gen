package com.gu.kindlegen

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class BookSectionSuite extends FlatSpec {
  ".chunkBookSectionPages" should "return list of list of BookSectionPages grouped by bookSectionId" in {
    val bookSectionPages = List(
      BookSectionPage("a", 1, List()),
      BookSectionPage("a", 1, List()),
      BookSectionPage("a", 1, List()),
      BookSectionPage("a", 1, List()),
      BookSectionPage("b", 1, List()),
      BookSectionPage("b", 1, List())
    )
    val result = BookSection.groupPagesIntoSections(bookSectionPages)
    val expected = List(
      List(
        BookSectionPage("a", 1, List()),
        BookSectionPage("a", 1, List()),
        BookSectionPage("a", 1, List()),
        BookSectionPage("a", 1, List())
      ),
      List(
        BookSectionPage("b", 1, List()),
        BookSectionPage("b", 1, List())
      )
    )
    result should contain theSameElementsAs expected
  }

  it should "return empty list when given an empty list" in {
    BookSection.groupPagesIntoSections(Nil) shouldBe empty
  }
}
