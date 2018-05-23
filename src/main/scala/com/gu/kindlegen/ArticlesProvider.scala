package com.gu.kindlegen

import scala.concurrent.Future


trait ArticlesProvider {
  def fetchArticles(): Future[Seq[Article]]
}
