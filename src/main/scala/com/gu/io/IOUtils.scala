package com.gu.io

import java.nio.file.{Files, Path}

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
}
