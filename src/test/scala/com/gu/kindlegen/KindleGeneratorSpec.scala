package com.gu.kindlegen

import java.nio.file.{Files, Paths}
import java.time.LocalDate

import scala.collection.JavaConverters._

import org.scalatest.FunSpec
import org.scalatest.Matchers._

import com.gu.kindlegen.KindleGenerator._


class KindleGeneratorSpec extends FunSpec {
  private val settings = Settings.load.get
  private def newInstance(editionDate: LocalDate) = new KindleGenerator(settings, editionDate)

  describe("fetchNitfBundle") {
    val arbitraryDate = LocalDate.of(2018, 4, 1)
    lazy val files = newInstance(arbitraryDate).fetchNitfBundle
    lazy val paths = files.map(_.path)

    it("returns some NITF files") {
      atLeast(MinArticlesPerEdition, paths) should endWith(".nitf")
    }

    it("returns some image files") {
      atLeast(10, paths) should (endWith(".gif") or endWith(".jpg") or endWith(".jpeg") or endWith(".png"))
    }
  }

  private val tmpDir = Files.createDirectories(Paths.get("target", "tmp"))
  private def writeFilesFor(editionDate: LocalDate) = {
    val outputDir = Files.createDirectories(tmpDir.resolve(editionDate.toString))

    val kindleGenerator = newInstance(editionDate)
    kindleGenerator.writeNitfBundleToDisk(outputDir)

    val generatedFiles = Files.list(outputDir).iterator.asScala.toList
    generatedFiles should not be empty
  }

  describe("writeNitfBundleToDisk") {
    val firstDate = LocalDate.of(2018, 4, 1)
    val lastDate = LocalDate.of(2018, 4, 1)
    (firstDate.toEpochDay to lastDate.toEpochDay).map(LocalDate.ofEpochDay).foreach { editionDate =>
      it(s"writes NITF bundles to disk for date $editionDate") {
        writeFilesFor(editionDate)
      }
    }
  }
}
