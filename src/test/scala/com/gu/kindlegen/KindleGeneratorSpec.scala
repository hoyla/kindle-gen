package com.gu.kindlegen

import java.nio.file.Files
import java.time.LocalDate

import org.scalatest.FunSpec
import org.scalatest.Inspectors._
import org.scalatest.Matchers._

import com.gu.scalatest.PathMatchers._


class KindleGeneratorSpec extends FunSpec {
  private val settings = Settings.load.get
  private def newInstance(editionDate: LocalDate) = KindleGenerator(settings, editionDate)

  describe("fetchNitfBundle") {
    val arbitraryDate = LocalDate.of(2018, 4, 1)
    lazy val files = newInstance(arbitraryDate).fetchNitfBundle
    lazy val paths = files.map(_.path)

    it("returns some NITF files") {
      atLeast(settings.publishing.minArticlesPerEdition, paths) should endWith(".nitf")
    }

    it("returns some image files") {
      atLeast(10, paths) should (endWith(".gif") or endWith(".jpg") or endWith(".jpeg") or endWith(".png"))
    }
  }

  private def writeFilesFor(editionDate: LocalDate) = {
    val kindleGenerator = newInstance(editionDate)
    val generatedFiles = kindleGenerator.writeNitfBundleToDisk()

    generatedFiles should not be empty
    forAll(generatedFiles) { path =>
      path should beAChildOf(settings.publishing.outputDir)
      withClue(path) { Files.readAllBytes(path) should not be empty }
    }
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
