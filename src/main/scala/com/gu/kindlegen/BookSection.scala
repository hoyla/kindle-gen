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

  def chunkBookSectionPages(listBSPs: List[BookSectionPage]): List[List[BookSectionPage]] =
    ListUtil.chunkBy(listBSPs, getBookSectionId)

  private def getBookSectionId(page: BookSectionPage): String =
    page.bookSectionId
}
