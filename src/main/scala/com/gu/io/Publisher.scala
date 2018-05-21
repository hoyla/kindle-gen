package com.gu.io

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.util.Success

import org.apache.logging.log4j.scala.Logging

import com.gu.concurrent.SideEffectsExecutionContext


trait Publisher extends Logging {
  /** Saves the content, asynchronously, eventually recording it in `publications` */
  def save(content: Array[Byte], fileName: String): Future[Link] = {
    saving(fileName)
    persist(content, fileName)
      .andThen { case Success(link) => saved(fileName, link) }(SideEffectsExecutionContext)
  }

  /** Signals that all content has been saved properly.
    *
    * This method should make the saved content accessible to consumers. It must be called _after_ all the futures
    * returned from `save` are complete.
    */
  def publish(): Future[Unit] = { logger.traceEntry(); Future.unit }

  def publications: Iterable[Link] = savedLinks.keys

  /** Records content that is to be published */
  protected def persist(content: Array[Byte], fileName: String): Future[Link]

  /** Called before content is persisted */
  protected def saving(key: String): Unit = { logger.trace(s"Saving $key...") }

  /** Called after content is persisted */
  protected def saved(fileName: String, link: Link): Unit = {
    logger.trace(s"Saved $fileName as $link")
    savedLinks.put(link, null)
  }

  // Map is the only concurrent collection type in Scala 2.12!
  protected val savedLinks: scala.collection.concurrent.Map[Link, Null] = TrieMap()
}
