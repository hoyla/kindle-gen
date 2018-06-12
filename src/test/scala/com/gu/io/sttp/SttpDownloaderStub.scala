package com.gu.io.sttp
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

import com.softwaremill.sttp
import com.softwaremill.sttp.{FutureMonad, MonadError, Response, SttpBackend}
import com.softwaremill.sttp.testing.SttpBackendStub


class SttpDownloaderStub(override val backend: SttpBackendStub[Future, Nothing]) extends SttpDownloader {
  override protected type StreamingCapability = Nothing
  override protected implicit def ec: ExecutionContext = global
}

object SttpDownloaderStub {
  type Backend = SttpBackendStub[Future, Nothing]

  def apply(specifications: Backend => Backend) =
    new SttpDownloaderStub(specifications(SttpBackendStub.asynchronousFuture))

  def withResponseBody[T](responseBody: T) =
    apply(_.whenAnyRequest.thenRespond(responseBody))

  def never = new SttpDownloader {
    override protected type StreamingCapability = Nothing
    override protected def ec: ExecutionContext = global
    override protected implicit def backend = new SttpBackend[Future, StreamingCapability] {
      override def close(): Unit = {}
      override val responseMonad: MonadError[Future] = new FutureMonad()

      override def send[T](request: sttp.Request[T, Nothing]): Future[Response[T]] = Future.never
    }
  }
}
