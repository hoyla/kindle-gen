package com.gu.io

import java.net.URL
import java.nio.file.{Files, Path, Paths}

import scala.util.Try
import scala.util.matching.Regex


object IOUtils {
  val unwantedFileNameChars: Regex = raw"""[:/\\?&#<>'"]""".r

  def asFileName(str: String): String = {
    unwantedFileNameChars.replaceAllIn(str, "_")
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

  def resourceAsPath(resourcePath: String): Option[Path] =
    resourceUrl(resourcePath)
      .flatMap { url =>
        Try { Paths.get(url.toURI) }.toOption
      }

  def resourceUrl(resourcePath: String): Option[URL] =
    classLoader.flatMap(cl => Option(cl.getResource(resourcePath)))

  private def classLoader: Option[ClassLoader] =
    Option(Thread.currentThread.getContextClassLoader)
      .orElse(Option(this.getClass.getClassLoader))
      .orElse(Option(ClassLoader.getSystemClassLoader))
}
