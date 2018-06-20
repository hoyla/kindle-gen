package com.gu.kindlegen


trait BookBinder {
  def group(articles: Seq[Article]): Seq[BookSection]
}
