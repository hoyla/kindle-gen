package com.gu.scalatest

import java.nio.file.{Files, Path}

import org.scalatest.matchers.{Matcher, MatchResult}

trait PathMatchers {
  def beTheSameFileAs(expected: Path): Matcher[Path] = (left: Path) => MatchResult(
    Files.isSameFile(left, expected),
    s"Path($left) did not locate the same file as Path($expected)",
    s"Path($left) located the same file as Path($expected)"
  )

  def beAChildOf(expectedParent: Path): Matcher[Path] = startWith(expectedParent)
  def startWith(expectedParent: Path): Matcher[Path] = (left: Path) => MatchResult(
    left.startsWith(expectedParent),
    s"Path($left) is not a child of Path($expectedParent)",
    s"Path($left) is a child of Path($expectedParent)"
  )
}

object PathMatchers extends PathMatchers
