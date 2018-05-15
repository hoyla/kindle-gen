package com.gu.io

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.util.Success

import com.gu.concurrent.SideEffectsExecutionContext


trait Publisher {
  /** Saves the content, asynchronously, eventually recording it in `publications` */
  def save(content: Array[Byte], fileName: String): Future[Link] = {
    saving(fileName)
    persist(content, fileName)
      .andThen { case Success(link) => saved(fileName, link) }(SideEffectsExecutionContext)
  }

  /** Signals that all content has been saved
    *
    * This method should make the saved content accessible to consumers.
    */
  def publish(): Future[Unit] = Future.unit

  def publications: Iterable[Link] = savedLinks.keys

  /** Records content that is to be published */
  protected def persist(content: Array[Byte], fileName: String): Future[Link]

  /** Called before content is persisted */
  protected def saving(key: String): Unit = {}

  /** Called after content is persisted */
  protected def saved(fileName: String, link: Link): Unit = {
    savedLinks.put(link, null)
  }

  protected val savedLinks: scala.collection.concurrent.Map[Link, Null] = TrieMap()
}
