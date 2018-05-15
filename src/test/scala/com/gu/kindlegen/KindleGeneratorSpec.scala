package com.gu.kindlegen

import java.nio.file.{Files, Path}
import java.time.LocalDate

import scala.concurrent.Await
import scala.xml.XML

import com.typesafe.config.ConfigFactory
import org.scalatest.FunSpec
import org.scalatest.Inspectors._
import org.scalatest.Matchers._

import com.gu.io.{FilePublisher, TempFiles}
import com.gu.io.Link.PathLink
import com.gu.scalatest.PathMatchers._
import com.gu.xml.XmlUtils._


class KindleGeneratorSpec extends FunSpec with TempFiles {
  private val settings = Settings.load.get
  private val fileSettings = settings.publishing.files

  private val conf = ConfigFactory.load.getConfig("KindleGeneratorSpec")
  private val deleteGeneratedFiles = conf.getBoolean("deleteGeneratedFiles")

  {
    val firstDate = LocalDate.now
    val lastDate = LocalDate.now
    (firstDate.toEpochDay to lastDate.toEpochDay).map(LocalDate.ofEpochDay).foreach(test)
  }

  def test(editionDate: LocalDate): Unit = describe(s"publish($editionDate)") {
    import scala.concurrent.ExecutionContext.Implicits.global
    val publisher = FilePublisher(fileSettings.outputDir.resolve(editionDate.toString))
    val generator = KindleGenerator(settings, editionDate, publisher)

    lazy val links = Await.result(generator.publish().map(_ => publisher.publications), settings.query.downloadTimeout)
    lazy val paths = links.collect { case x: PathLink => x.toPath }.toSeq
    lazy val fileNames = paths.map(_.getFileName.toString)

    lazy val rssFiles = pathsEndingWith(fileSettings.rssExtension)
    lazy val nitfFiles = pathsEndingWith(fileSettings.nitfExtension)

    def pathsEndingWith(fileNameSuffix: String) =
      paths.filter(_.getFileName.toString.endsWith(fileNameSuffix))


    it("returns the generated files") {
      // execute the method under test inside a test case by evaluating (the lazy vals) `paths` and/or `fileNames`
      if (deleteGeneratedFiles) paths.foreach(trackTempFile)
      fileNames should not be empty

      forEvery(paths) { path =>
        path should beAChildOf(fileSettings.outputDir)
        path.toFile should exist
        withClue(path) { path.toFile.length.toInt should be > 0 }  // file shouldn't be empty and shouldn't be larger than 2GB
      }
    }

    if (settings.publishing.downloadImages) it("returns some image files") {
      atLeast(10, fileNames) should (endWith(".gif") or endWith(".jpg") or endWith(".jpeg") or endWith(".png"))
    }

    it("generates some NITF files") {
      atLeast(settings.publishing.minArticlesPerEdition, fileNames) should endWith(fileSettings.nitfExtension)
    }

    it("generates valid NITF files") {
      forEvery(nitfFiles) { path => withClue(path) {
        val bareNitf = XML.loadFile(path.toFile)
        val nitf = ArticleNITF.qualify(bareNitf)  // specify the `xmlns` to validate against
        validateXml(nitf, resource("kpp-nitf-3.5.7.xsd").toURI)
      }}
    }

    it("generates some RSS files") {
      atLeast(3, fileNames) should endWith(fileSettings.rssExtension)
    }

    it("generates RSS files linking to all articles") {
      assertLinkedFilesCoverAllLinkableFiles(rssFiles.flatMap(linkedFiles), linkables = nitfFiles)
    }

    it("generates one root RSS file") {
      exactly(1, fileNames) shouldBe fileSettings.rootManifestFileName
    }

    it("generates a root RSS file linking to all other RSS files") {
      val rootManifestPath = pathsEndingWith(fileSettings.rootManifestFileName).head

      val linkedManifests = linkedFiles(rootManifestPath)
      val otherManifests = rssFiles.filterNot(_ == rootManifestPath)

      assertLinkedFilesCoverAllLinkableFiles(linkedManifests, otherManifests)
    }

    def linkedFiles(manifestPath: Path) = { withClue(manifestPath) {
      val xml = XML.loadFile(manifestPath.toFile)
      xml.label shouldBe "rss"

      val items = xml \ "channel" \ "item" \ "link"
      items should not be empty

      items.map(_.text.trim).map(manifestPath.resolveSibling)
    }}

    def assertLinkedFilesCoverAllLinkableFiles(linkedFiles: Seq[Path], linkables: Seq[Path]) = {
      assume(linkables.forall(Files.exists(_)))
      forEvery(linkedFiles) { _.toFile should exist }
      linkedFiles.map(_.toRealPath()) should contain theSameElementsAs linkables.map(_.toRealPath())
    }
  }
}
