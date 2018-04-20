package com.gu.io

import java.nio.file.Files

import org.scalatest.FunSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import scala.concurrent.ExecutionContext.Implicits.global

class IOUtilsSpec extends FunSpec with ScalaFutures with IntegrationPatience {
  import IOUtils._

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

    it("downloads data from a URL into a file") {
      val tempFile = Files.createTempFile(null, null)

      val downloadedFile = downloadAs(tempFile, sampleUrl).futureValue

      Files.isSameFile(downloadedFile, tempFile) shouldBe true
      Files.readAllBytes(downloadedFile) shouldBe sampleContents
    }
  }
}