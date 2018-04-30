package com.gu.io

import java.nio.file.{Files, Path}

import scala.concurrent.{ExecutionContext, Future}

import scalaj.http._

object IOUtils {
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
      val response = request.asBytes.throwError
      response.body
    }
  }

  /** Downloads a URL to the file denoted by `path`.
    * Creates the parent directory if it doesn't exist and overwrites the file if it exists.
    */
  def downloadAs(path: Path, url: String)(implicit ec: ExecutionContext): Future[Path] = {
    Future {
      val absolutePath = path.toAbsolutePath
      val directory = Files.createDirectories(absolutePath.getParent)
      directory.resolve(absolutePath.getFileName)
    }.flatMap { downloadPath =>
      download(url).map(bytes => Files.write(downloadPath, bytes))
    }
  }
}
