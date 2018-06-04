package com.gu.io

import java.nio.file.{Files, Path}

import org.apache.logging.log4j.scala.Logging

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
}
