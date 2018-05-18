package com.gu.io

import java.nio.file.{Files, Path}

import scala.concurrent.{ExecutionContext, Future}

import org.apache.logging.log4j.scala.Logging
import scalaj.http._

object IOUtils extends Logging {
  def asFileName(str: String): String = {
    str.replaceAll(raw"[:/\\?&#]", "_")
  }

  def fileExtension(pathOrUrl: String): String = {
    pathOrUrl.substring(pathOrUrl.lastIndexOf('.') + 1)
  }

  /** Deletes files and directories */
  def deleteRecursively(path: Path): Boolean = {
    import Files._
    if (isDirectory(path))
      walk(path).filter(_ != path).forEach(deleteRecursively)
    deleteIfExists(path)
  }

  def download(url: String)(implicit ec: ExecutionContext): Future[Array[Byte]] = {
    val request = Http(url)
    Future {
      logger.debug(s"Downloading $url...")
      val response = request.asBytes.throwError
      logger.debug(s"Downloaded $url: HTTP ${response.code}")
      logger.trace(s"Downloaded $url: $response")
      response.body
    }
  }

  /** Downloads a URL to the file denoted by `path`.
    * Creates the parent directory if it doesn't exist and overwrites the file if it exists.
    */
  def downloadAs(path: Path, url: String)(implicit ec: ExecutionContext): Future[Path] = {
    Future {
      val absolutePath = path.toAbsolutePath
      logger.debug(s"Creating parent directory of $absolutePath...")
      val directory = Files.createDirectories(absolutePath.getParent)
      directory.resolve(absolutePath.getFileName)
    }.flatMap { downloadPath =>
      download(url).map { bytes =>
        logger.debug(s"Writing $url to $downloadPath...")
        Files.write(downloadPath, bytes)
      }
    }
  }
}
