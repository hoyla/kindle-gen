package com.gu.io

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.Success

import org.apache.logging.log4j.scala.Logging

import com.gu.concurrent.SideEffectsExecutionContext


trait Publisher extends AutoCloseable with Logging {
  type PublishedLink <: Link

  /** Saves the content, asynchronously, eventually recording it in `publications`.
    *
    * After (trying to) save all content, whether or not the result is successful, you must call `close` to free up
    * resources.
    */
  def save(content: Array[Byte], fileName: String): Future[PublishedLink] = {
    saving(fileName)
    persist(content, fileName)
      .andThen { case Success(link) => saved(fileName, link) }(SideEffectsExecutionContext)
  }

  /** Signals that all content has been saved properly.
    *
    * This method should make the saved content accessible to consumers. It must be called _after_ all the futures
    * returned from `save` are complete.
    *
    * @see close
    */
  def publish(): Future[Unit] = { logger.traceEntry(); Future.unit }

  def close(): Unit =  { }

  def publications: Iterable[PublishedLink] = savedLinks.asScala

  /** Records content that is to be published */
  protected[this] def persist(content: Array[Byte], fileName: String): Future[PublishedLink]

  /** Called before content is persisted */
  protected[this] def saving(key: String): Unit = { logger.trace(s"Saving $key...") }

  /** Called after content is persisted */
  protected[this] def saved(fileName: String, link: PublishedLink): Unit = {
    logger.trace(s"Saved $fileName as $link")
    savedLinks.add(link)
  }

  protected val savedLinks = new java.util.concurrent.ConcurrentLinkedQueue[PublishedLink]()
}
