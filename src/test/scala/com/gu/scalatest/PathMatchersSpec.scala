package com.gu.scalatest

import java.nio.file.{FileSystems, Paths}

import scala.collection.JavaConverters._

import org.scalatest.FunSpec
import org.scalatest.Inspectors._
import org.scalatest.Matchers._

class PathMatchersSpec extends FunSpec with PathMatchers {
  import Paths.{get => path}

  describe("beTheSameFileAs") {
    it("matches different paths pointing to the same file") {
      path("target") should beTheSameFileAs(path( ".", "target", "..", "target", "."))
    }
  }

  describe("beAChildOf(startWith)") {
    it("matches simple paths") {
      path("target", "scala-2.12") should beAChildOf(path("target"))
    }

    it("matches distant parents") {
      path("target", "scala-2.12", "classes", "com", "gu") should beAChildOf(path("target"))
    }

    it("matches relative paths") {
      path(".", "target") should beAChildOf(path("."))
      path(".", "target", "..", "target", ".") should beAChildOf(path("."))
      path("target", "scala-2.12", "..", "scala-2.12") should beAChildOf(path("target"))
    }

    it("matches relative paths against absolute paths") {
      forExactly(1, roots) { root =>
        path(".") should beAChildOf(root)
        path("target") should beAChildOf(root)
      }
    }

    it("matches non-existing paths") {
      path(".", "non-existing") should beAChildOf(path("."))
    }

    it("doesn't match the same path") {
      path("target") shouldNot beAChildOf(path("target"))
      path(".") shouldNot beAChildOf(path("."))
      path("..") shouldNot beAChildOf(path(".", ".", ".."))
    }

    it("doesn't match a sibling") {
      path("target") shouldNot beAChildOf(path("src"))
      path("target", "scala-2.12") shouldNot beAChildOf(path("target", "streams"))
    }

    it("doesn't match a parent") {
      path("target") shouldNot beAChildOf(path("target", "scala-2.12"))
    }
  }

  private def roots = FileSystems.getDefault.getRootDirectories.asScala
}
