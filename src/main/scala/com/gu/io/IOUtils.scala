package com.gu.io

import java.net.URL
import java.nio.file.{Files, Path, Paths}

import scala.io.Source
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

  /** Tries to find the resource on the local file system */
  def resourceAsPath(resourcePath: String): Option[Path] =
    resourceUrl(resourcePath)
      .flatMap { url =>
        Try { Paths.get(url.toURI) }.toOption
      }

  def resourceAsString(resourcePath: String): Option[String] =
    resourceUrl(resourcePath)
      .map { url =>
        val source = Source.fromURL(url)
        try { source.mkString } finally { source.close }
      }

  def resourceUrl(resourcePath: String): Option[URL] =
    classLoader.flatMap(cl => Option(cl.getResource(resourcePath)))

  private def classLoader: Option[ClassLoader] =
    Option(Thread.currentThread.getContextClassLoader)
      .orElse(Option(this.getClass.getClassLoader))
      .orElse(Option(ClassLoader.getSystemClassLoader))
}
