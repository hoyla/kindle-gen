package com.gu.io

import java.nio.file.{Files, Path}

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.FunSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

import com.gu.scalatest.PathMatchers._


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

  describe("download") {
    // this is, effectively, an integration test

    val sampleUrl = "https://dev.w3.org/SVG/tools/svgweb/samples/svg-files/decimal.svg"
    val sampleContents =
      """<svg viewBox='0 0 125 80' xmlns='http://www.w3.org/2000/svg'>
        |  <text y="75" font-size="100" font-family="serif"><![CDATA[10]]></text>
        |</svg>
        |""".stripMargin.getBytes("UTF-8")

    it("downloads data from a URL into memory") {
      download(sampleUrl).futureValue shouldBe sampleContents
    }

    def testDownloadAs(tempFile: Path) = {
      val downloadedFile = downloadAs(tempFile, sampleUrl).futureValue

      downloadedFile should beTheSameFileAs(tempFile)
      Files.readAllBytes(downloadedFile) shouldBe sampleContents
    }

    it("downloads data from a URL into a file") {
      testDownloadAs(newTempFile)
    }

    it("downloads data from a URL into a file in a new directory") {
      val tempDir = newTempDir
      val newDir = tempDir.resolve("newdir")
      val newFile = newDir.resolve("newfile")

      testDownloadAs(newFile)
    }
  }
}
