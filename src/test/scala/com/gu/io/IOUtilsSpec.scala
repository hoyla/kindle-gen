package com.gu.io

import org.scalatest.FunSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}


class IOUtilsSpec extends FunSpec with ScalaFutures with IntegrationPatience with TempFiles {
  import IOUtils._

  describe("asFileName") {
    it("converts invalid filename characters to underscores") {
      asFileName(raw"http://example.com\file?a=0&i=9#anchor") shouldBe "http___example.com_file_a=0_i=9_anchor"
    }
  }

  describe("fileExtension") {
    it("can process an empty string") { fileExtension("") shouldBe "" }
    it("extracts a 3-chars extension") { fileExtension("image.png") shouldBe "png" }
    it("extracts a multi-char extension") { fileExtension("example.desktop") shouldBe "desktop" }

    it("extracts the extension from a path") { fileExtension("a/b/c.d") shouldBe "d" }
    it("extracts the extension from a URL domain") { fileExtension("http://example.com") shouldBe "com" }
    it("extracts the extension from a URL path") { fileExtension("http://example.com/my.file") shouldBe "file" }

    it("extracts the extension when the file name is missing") {
      fileExtension(".DS_Store") shouldBe "DS_Store"
    }

    it("ignores file names when the extension is missing") {
      fileExtension("fileNameEndingWithDot.") shouldBe ""
    }

    it("ignores file names that don't contain a dot") {
      pendingUntilFixed { fileExtension("fileWithoutExtension") shouldBe "" }
    }
  }
}
