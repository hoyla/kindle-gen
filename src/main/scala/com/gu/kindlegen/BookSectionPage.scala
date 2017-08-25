package com.gu.kindlegen

/**
 * Each Section (e.g. Finance or Top Stories) has a number of pages, each of which can contain one or more articles.
 */
case class BookSectionPage(bookSectionId: String, pageNum: Int, articles: List[Article])

// TODO: Ask David B if I should group by BookSectionPage as well as page number - as in, can a page have two book sections in?
// TODO: allow edge case where a page can have more than one book section on it.

