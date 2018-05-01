package com.gu.kindlegen

import java.nio.file.{Files, Path}
import java.time.LocalDate

import scala.xml.XML

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
    lazy val paths = newInstance(arbitraryDate).writeNitfBundleToDisk()
    lazy val fileNames = paths.map(_.getFileName.toString)
    lazy val rssFiles = pathsWithSuffix(fileSettings.rssExtension)

    def pathsWithSuffix(fileNameSuffix: String) =
      paths.filter(_.getFileName.toString.endsWith(fileNameSuffix))


    it("works") {
      fileNames should not be empty  // execute the method under test inside a test case by evaluating the lazy val `fileNames`
    }

    if (settings.publishing.downloadImages) it("returns some image files") {
      atLeast(10, fileNames) should (endWith(".gif") or endWith(".jpg") or endWith(".jpeg") or endWith(".png"))
    }

    it("returns some NITF files") {
      atLeast(settings.publishing.minArticlesPerEdition, fileNames) should endWith(fileSettings.nitfExtension)
    }

    it("returns some RSS files") {
      atLeast(3, fileNames) should endWith(fileSettings.rssExtension)
      testManifests(rssFiles)

      val linkedArticles = rssFiles.flatMap(linkedFiles)
      val allArticles = pathsWithSuffix(fileSettings.nitfExtension)
      testLinkedFilesCoverAllLinkableFiles(linkedArticles, allArticles)
    }

    it("returns one root RSS file") {
      exactly(1, fileNames) shouldBe fileSettings.rootManifestFileName

      val rootManifestPath = pathsWithSuffix(fileSettings.rootManifestFileName).head
      testManifests(Seq(rootManifestPath))

      val linkedManifests = linkedFiles(rootManifestPath)
      val otherManifests = rssFiles.filterNot(_.getFileName.toString == fileSettings.rootManifestFileName)
      testLinkedFilesCoverAllLinkableFiles(linkedManifests, otherManifests)
    }

    def testManifests(manifests: Seq[Path]) = {
      forAll(manifests) { manifest =>
        manifest.toFile should exist
        forAll(linkedFiles(manifest)) { linkedFile =>
          linkedFile.toFile should exist
        }
      }
    }

    def linkedFiles(manifestPath: Path) = {
      val xml = XML.loadFile(manifestPath.toFile)
      xml.label shouldBe "rss"

      val items = xml \ "channel" \ "item" \ "link"
      items should not be empty

      items.map(_.text).map(manifestPath.resolveSibling)
    }

    def testLinkedFilesCoverAllLinkableFiles(linkedFiles: Seq[Path], linkable: Seq[Path]) =
      linkedFiles.map(_.toRealPath())  should contain theSameElementsAs linkable.map(_.toRealPath())
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
