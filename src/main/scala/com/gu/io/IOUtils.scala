package com.gu.io

import scala.util.matching.Regex


object IOUtils {
  val unwantedFileNameChars: Regex = raw"""[:/\\?&#<>'"]""".r

  def asFileName(str: String): String = {
    unwantedFileNameChars.replaceAllIn(str, "_")
  }

  def fileExtension(pathOrUrl: String): String = {
    pathOrUrl.substring(pathOrUrl.lastIndexOf('.') + 1)
  }
}
