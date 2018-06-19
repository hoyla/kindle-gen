package com.gu.kindlegen

import scala.concurrent.Future
import scala.collection.immutable

import com.gu.concurrent.SideEffectsExecutionContext


trait ArticlesProvider {
  def fetchArticles(): Future[Seq[Article]]
}

final class CompositeArticlesProvider(providers: ArticlesProvider*) extends ArticlesProvider {
  require(providers.nonEmpty, "providers must not be empty!")

  override def fetchArticles(): Future[Seq[Article]] = {
    val groupsOfEventualArticles =
      providers
        .map(_.fetchArticles())
        .to[immutable.Iterable]

    Future.reduceLeft(groupsOfEventualArticles)(_ ++ _)(SideEffectsExecutionContext)
  }
}
