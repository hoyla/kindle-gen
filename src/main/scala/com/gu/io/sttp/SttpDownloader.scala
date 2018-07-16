package com.gu.io.sttp

import java.nio.file.Path

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import com.softwaremill.sttp._
import org.apache.logging.log4j.scala.Logging

import com.gu.io.Downloader


trait SttpDownloader extends Downloader with Logging {
  protected type StreamingCapability
  protected implicit def backend: SttpBackend[Future, StreamingCapability]
  protected implicit def ec: ExecutionContext

  type Request[T] = com.softwaremill.sttp.Request[T, StreamingCapability]

  override def download(url: String): Future[Array[Byte]] = {
    val request = sttp.get(uri"$url").response(asByteArray)
    download(request)
  }

  override def downloadAs(path: Path, url: String): Future[Path] = {
    val request = sttp.get(uri"$url").response(asPath(path, overwrite = true))
    download(request)
  }

  def download[T](request: Request[T]): Future[T] = {
    send(request)
      .map { response => response.unsafeBody }
  }

  def send[T](request: Request[T]): Future[Response[T]] = {
    logger.trace(s"Downloading $request")
    request.send[Future]()
      .andThen(logResponse(request))
  }

  protected def logResponse(request: Request[_]): PartialFunction[Try[Response[_]], Unit] = {
    case Success(response: Response[_]) =>
      if (response.isSuccess) {
        logger.debug(s"Downloaded ${request.uri}: HTTP ${response.code}")
        logger.trace(s"Downloaded $request: $response")
      } else {
        logger.debug(s"Failed to download ${request.uri}: HTTP ${response.code}! $response")
      }
    case Failure(error) =>
      logger.debug(s"Could not download ${request.uri}! $error")
  }
}
