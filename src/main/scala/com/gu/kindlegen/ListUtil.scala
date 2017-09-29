package com.gu.kindlegen

/**
 * Extra utility functions for working with Lists
 */
object ListUtil {
  /**
   * Splits a list into chunks based upon a group identifier.
   * getGroupId is the function that determines what you want to chunk according to.
   * Span is a Scala equivalent of (collection.takeWhile(predicate), collection.dropWhile(predicate)).
   * Span has the following type signature:
   * span(pred: A => Boolean): (List[A], List[A])
   * Note that the collection should be ordered first. (Order will then be preserved)
   */
  def chunkBy[A, B](collection: List[A], getGroupId: A => B): List[List[A]] = {
    if (collection.isEmpty) return List()
    val id = getGroupId(collection.head)
    val (chunk, rest) = collection.span(a => getGroupId(a) == id)
    chunk :: chunkBy(rest, getGroupId)
  }
}
