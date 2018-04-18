package com.gu.kindlegen

import java.nio.file.{Files, Paths}
import java.time.LocalDate

import scala.collection.JavaConverters._

import org.scalatest.FunSpec
import org.scalatest.Matchers._

class KindleGeneratorSpec extends FunSpec {
  private val settings = Settings.load.get

  describe("KindleGenerator") {
    val tmp = Files.createDirectories(Paths.get("target", "tmp"))
    def generateFilesFor(editionDate: LocalDate) = {
      val outputDir = Files.createDirectories(tmp.resolve(editionDate.toString))

      val kindleGenerator = new KindleGenerator(settings, editionDate)
      kindleGenerator.getNitfBundleToDisk(outputDir)

      val generatedFiles = Files.list(outputDir).iterator.asScala.toList
      generatedFiles should not be empty
    }

    val firstDate = LocalDate.of(2018, 4, 1)
    val lastDate = LocalDate.of(2018, 4, 1)
    (firstDate.toEpochDay to lastDate.toEpochDay).map(LocalDate.ofEpochDay).foreach { editionDate =>
      it(s"writes NITF bundles to disk for date $editionDate") {
        generateFilesFor(editionDate)
      }
    }
  }
}
