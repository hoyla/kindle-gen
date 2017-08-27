package com.gu.kindlegen

/**
 * Each Book (eg Guardian or Observer) contains many sections (eg G2, Top Stories, Finance) each of which will have a number of Pages. Each Page can then contain one or more articles
 */
case class BookSection(bookSectionId: String, bookSectionTitle: String, pages: List[BookSectionPage]) {
  def minPage = pages.map(_.pageNum).min
  def maxPage = pages.map(_.pageNum).max
  def numPages = pages.size
  def numArticles = pages.flatMap(_.articles).size
}

object BookSection {

  def chunkBookSectionPages(pages: List[BookSectionPage]): List[List[BookSectionPage]] = {
    val tsil = pages.foldLeft(List.empty[List[BookSectionPage]]) {
      // case where the id of the first page in the first list is the same as current enum page id.
      case ((head :: tail), page) if head.map(_.bookSectionId).head == page.bookSectionId => {
        (page :: head) :: tail
      }
      // case where ids do not match (ie the same pattern as above but not caught by the above case if statement) This pattern will also catch the empty initial list
      case (lst, page) =>
        List(page) :: lst
    }
    tsil.reverse
  }

}
