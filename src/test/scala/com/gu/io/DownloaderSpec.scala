package com.gu.io

import java.nio.file.{Files, Path}

import org.scalatest.FunSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

import com.gu.scalatest.PathMatchers._


abstract class DownloaderSpec(downloader: Downloader) extends FunSpec with ScalaFutures with IntegrationPatience with TempFiles {
  import downloader._

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
