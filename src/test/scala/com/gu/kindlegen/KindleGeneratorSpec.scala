package com.gu.kindlegen

import java.nio.file.Files
import java.time.LocalDate

import com.typesafe.config.ConfigFactory
import org.scalatest.FunSpec
import org.scalatest.Inspectors._
import org.scalatest.Matchers._

import com.gu.scalatest.PathMatchers._


class KindleGeneratorSpec extends FunSpec {
  private val settings = Settings.load.get
  private val fileSettings = settings.publishing.files
  private def newInstance(editionDate: LocalDate) = KindleGenerator(settings, editionDate)

  private val conf = ConfigFactory.load.getConfig("KindleGeneratorSpec")
  private val deleteGeneratedFiles = conf.getBoolean("deleteGeneratedFiles")

  describe("writeNitfBundle") {
    val arbitraryDate = LocalDate.of(2018, 4, 1)
    lazy val paths = newInstance(arbitraryDate).writeNitfBundleToDisk().map(_.getFileName.toString)

    it("works") {
      paths should not be empty  // execute the method under test inside a test case by evaluating the lazy val `paths`
    }

    it("returns some NITF files") {
      atLeast(settings.publishing.minArticlesPerEdition, paths) should endWith(fileSettings.nitfExtension)
    }

    it("returns some RSS files") {
      atLeast(3, paths) should endWith(fileSettings.rssExtension)
    }

    it("returns one root manifest file") {
      exactly(1, paths) shouldBe fileSettings.rootManifestFileName
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
      path should beAChildOf(fileSettings.outputDir)
      withClue(path) { Files.readAllBytes(path) should not be empty }
    }

    if (deleteGeneratedFiles) generatedFiles.foreach(Files.delete)
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
