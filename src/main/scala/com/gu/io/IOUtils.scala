package com.gu.io

import java.nio.file.{Files, Path}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import scalaj.http._

object IOUtils {
  def fileExtension(pathOrUrl: String): String = {
    pathOrUrl.substring(pathOrUrl.lastIndexOf('.') + 1)
  }

  def download(url: String)(implicit ec: ExecutionContext): Future[Array[Byte]] = {
    val request = Http(url)
    Future {
      val response = request.asBytes.throwError
      response.body
    }
  }

  /** Downloads a URL to the file denoted by `path`.
    * Creates the parent directory if it doesn't exist and overwrites the file if it exists.
    */
  def downloadAs(path: Path, url: String)(implicit ec: ExecutionContext): Future[Path] = {
    Future.fromTry(Try(
      Option(path.toRealPath().getParent).foreach(Files.createDirectories(_))
    )).flatMap { _ =>
      download(url).map(bytes => Files.write(path, bytes))
    }
  }
}
