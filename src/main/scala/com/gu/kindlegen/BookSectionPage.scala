package com.gu.kindlegen

/**
 * Each Section (e.g. Finance or Top Stories) has a number of pages, each of which can contain one or more articles.
 */
case class BookSectionPage(bookSectionId: String, pageNum: Int, articles: List[Article])

// TODO: Ask DB if the order INSIDE the pages matters (ie order of the articles) and if so how I recreate this.

object BookSectionPage {

  def chunkByPageNum(articles: List[Article]): List[List[Article]] = {
    // TODO: sort articles in/before responseToArticles method
    val sortedArticles = articles.sortBy(_.newspaperPageNumber)

    ListUtil.chunkBy(sortedArticles, getNewspaperPageNumber)
  }

  private def getNewspaperPageNumber(article: Article): Int = article.newspaperPageNumber

  def chunksToBSP(chunks: List[List[Article]]): List[BookSectionPage] = {
    chunks.map(lst => {
      BookSectionPage(bookSectionId = Option(lst.head.newspaperBookSection).getOrElse(""), pageNum = Option(lst.head.newspaperPageNumber).getOrElse(0), articles = lst)
    })
  }
}