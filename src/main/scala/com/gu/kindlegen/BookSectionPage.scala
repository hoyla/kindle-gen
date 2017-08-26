package com.gu.kindlegen

/**
 * Each Section (e.g. Finance or Top Stories) has a number of pages, each of which can contain one or more articles.
 */
case class BookSectionPage(bookSectionId: String, pageNum: Int, articles: List[Article])

// TODO: Ask David B if I should group by BookSectionPage as well as page number - as in, can a page have two book sections in?
// TODO: allow edge case where a page can have more than one book section on it.
// TODO: Ask DB if the order INSIDE the pages matters (ie order of the articles) and if so how I recreate this.

//
object BookSectionPage {
  def chunkByPageNum(articles: List[Article]): List[List[Article]] = {
    val tsil = articles.foldLeft(List.empty[List[Article]]) {
      // case where the pagenum of the first article in the first list is the same as current enum article pagenum.
      case ((head :: tail), article) if head.map(_.newspaperPageNumber).head == article.newspaperPageNumber => {
        (article :: head) :: tail
      }
      // case where pagenums do not match (ie the same pattern as above but not caught by the above case if statement) This pattern will also catch the empty initial list
      case (listDifferentPage, article) =>
        List(article) :: listDifferentPage
    }
    tsil.reverse
  }

  def chunksToBSP(chunks: List[List[Article]]): List[BookSectionPage] = {
    chunks.map(lst => {
      BookSectionPage(bookSectionId = Option(lst.head.newspaperBookSection).getOrElse(""), pageNum = Option(lst.head.newspaperPageNumber).getOrElse(0), articles = lst)
    })
  }
}