package com.gu.scalatest

import java.nio.file.{Files, Path}

import org.scalatest.matchers.{Matcher, MatchResult}

trait PathMatchers {
  def beTheSameFileAs(expected: Path) = new PathsLocateSameFileMatcher(expected)

  class PathsLocateSameFileMatcher(expected: Path) extends Matcher[Path] {
    override def apply(left: Path): MatchResult = MatchResult(
      Files.isSameFile(left, expected),
      s"Path($left) did not locate the same file as Path($expected)",
      s"Path($left) located the same file as Path($expected)"
    )
  }
}

object PathMatchers extends PathMatchers
