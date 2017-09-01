package com.gu.kindlegen

import org.scalatest.FlatSpec

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
    val result = BookSection.chunkBookSectionPages(bookSectionPages)
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
    assert(result === expected)
  }

  it should "return empty list when given an empty list" in {
    assert(BookSection.chunkBookSectionPages(Nil) === List())
  }
}
