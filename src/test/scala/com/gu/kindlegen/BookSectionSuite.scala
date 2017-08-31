package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.github.nscala_time.time.Imports._
import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.client.utils._

@RunWith(classOf[JUnitRunner])
class BookSectionSuite extends FunSuite {
  test("chunkBookSectionPages") {
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

  test("chunkBookSectionPages with empty list") {
    assert(BookSection.chunkBookSectionPages(Nil) === List())
  }
}
