package com.gu.kindlegen

import scala.concurrent.Future

import com.gu.concurrent.SideEffectsExecutionContext


trait ArticlesProvider {
  def fetchArticles(): Future[Seq[Article]]
}

final class CompositeArticlesProvider(providers: ArticlesProvider*) extends ArticlesProvider {
  require(providers.nonEmpty, "providers must not be empty!")

  override def fetchArticles(): Future[Seq[Article]] =
    providers
      .map(_.fetchArticles())
      .reduce { (eventualArticles1, eventualArticles2) =>
        eventualArticles1.zipWith(eventualArticles2)(_ ++ _)(SideEffectsExecutionContext)
      }
}
