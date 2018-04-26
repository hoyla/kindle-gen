package com.gu.scalatest

import java.nio.file.{Files, Path}

import scala.util.Try

import org.scalatest.matchers.{Matcher, MatchResult}

trait PathMatchers {
  def beTheSameFileAs(expected: Path): Matcher[Path] = (left: Path) => MatchResult(
    Files.isSameFile(left, expected),
    s"Path($left) did not locate the same file as Path($expected)",
    s"Path($left) located the same file as Path($expected)"
  )

  /** Asserts that a path is a direct or indirect descendant of the expected parent */
  def beAChildOf(expectedParent: Path): Matcher[Path] = (left: Path) => {
    def isAChildOf(parent: Path, child: Path) = child != parent && child.startsWith(parent)

    MatchResult(
      // Unfortunately, Paths.get("/tmp").startsWith(Paths.get("/tmp/..")) yields false
      isAChildOf(expectedParent, left) ||
        isAChildOf(expectedParent.toAbsolutePath, left.toAbsolutePath) ||
        Try(isAChildOf(expectedParent.toRealPath(), left.toRealPath())).getOrElse(false),
      s"Path($left) is not a child of Path($expectedParent)",
      s"Path($left) is a child of Path($expectedParent)"
    )
  }
}

object PathMatchers extends PathMatchers
