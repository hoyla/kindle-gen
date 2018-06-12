package com.gu.concurrent

import scala.concurrent.{ExecutionContext, Future}


object FutureUtils {
  /** Filters out the failed futures and combines the successful results, skipping failures.
    *
    * You can specify whether failures should be ignored completely or reported to the execution context.
    * This enables you to be notified of failures (e.g. to log them). However, if the execution context throws back
    * an exception when it is notified then this method will return a failed future. This is consistent with the
    * behaviour of [[scala.concurrent.Future.andThen]].
    *
    * @tparam T the type of the values inside the futures
    * @param futures the `TraversableOnce` of Futures which will be sequenced
    * @param reportFailures whether to report failed futures to [[scala.concurrent.ExecutionContext.reportFailure]]
    * @return the `Future` of the sequence of successful results
    */
  def successfulSequence[T](futures: Seq[Future[T]], reportFailures: Boolean = true)
                           (implicit ec: ExecutionContext): Future[Seq[T]] = {
    Future.sequence {
      futures.map { future: Future[T] =>
        future.map(value => Some(value))
          .recover { case t: Throwable =>
            if (reportFailures)
              ec.reportFailure(t)  // if the execution context throws the exception then we'll return a failure!
            None
          }
      }
    }.map { wrappedValues: Seq[Option[T]] =>
      wrappedValues.flatten
    }
  }
}
