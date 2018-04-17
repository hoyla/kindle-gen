package com.gu.kindlegen

import java.nio.file.{Files, Paths}
import java.time.Instant

import scala.collection.JavaConverters._

import org.scalatest.FunSpec
import org.scalatest.Matchers._

class KindleGeneratorSpec extends FunSpec {
  private val settings = Settings.load.get

  describe("KindleGenerator") {
    it("writes NITF bundles to disk") {
      val tmp = Files.createDirectories(Paths.get("target", "tmp"))
      val outputDir = Files.createTempDirectory(tmp, "")
      info(s"writing NITF bundles to $outputDir")

      val editionDate = Instant.parse("2017-12-25T00:00:00Z")  // small edition with few articles
      val kindleGenerator = new KindleGenerator(settings, editionDate)
      kindleGenerator.getNitfBundleToDisk(outputDir)

      val generatedFiles = Files.list(outputDir).iterator.asScala.toList
      generatedFiles should not be empty
    }
  }
}