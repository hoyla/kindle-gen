package com.gu.kindlegen

import java.time.LocalDate
import java.time.ZoneOffset.UTC

import scala.concurrent.Await
import scala.xml.XML

import better.files._
import com.typesafe.config.ConfigFactory
import org.scalatest.FunSpec
import org.scalatest.Inspectors._
import org.scalatest.Matchers._

import com.gu.io.{FilePublisher, TempFiles}
import com.gu.io.Link.PathLink
import com.gu.io.sttp.OkHttpSttpDownloader
import com.gu.kindlegen.accuweather.AccuWeatherClient
import com.gu.kindlegen.app.Settings
import com.gu.kindlegen.capi.GuardianArticlesProvider
import com.gu.kindlegen.weather.DailyWeatherForecastProvider
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
    val downloader = OkHttpSttpDownloader()
    val weatherClient = AccuWeatherClient(settings.accuWeather, downloader)
    val publisher = FilePublisher(fileSettings.outputDir.resolve(editionDate.toString))
    val capiProvider = GuardianArticlesProvider(settings.contentApi, settings.articles, downloader, editionDate)
    val weatherProvider = new DailyWeatherForecastProvider(
      weatherClient, settings.weather.sections(editionDate.getDayOfWeek), editionDate.atStartOfDay.atOffset(UTC), settings.weather)
    val provider = new CompositeArticlesProvider(capiProvider, weatherProvider)
    val binder = MainSectionsBookBinder(settings.books.mainSections)
    val generator = new KindleGenerator(provider, binder, publisher, downloader, settings.articles.downloadTimeout, settings.publishing)

    def publish() = generator.publish().andThen { case _ => publisher.close() }
    lazy val links = Await.result(publish().map(_ => publisher.publications), settings.articles.downloadTimeout)
    lazy val files = links.collect { case x: PathLink => File(x.toPath) }.toSeq
    lazy val fileNames = files.map(_.name)

    lazy val rssFiles = filesEndingWith(fileSettings.rssExtension)
    lazy val nitfFiles = filesEndingWith(fileSettings.nitfExtension)

    def filesEndingWith(fileNameSuffix: String) =
      files.filter(_.name.endsWith(fileNameSuffix))


    it("returns the generated files") {
      // execute the method under test inside a test case by evaluating (the lazy vals) `paths` and/or `fileNames`
      if (deleteGeneratedFiles) files.foreach(trackTempFile)
      fileNames should not be empty

      forEvery(files) { file =>
        file.path should beAChildOf(fileSettings.outputDir)
        file.toJava should exist
        withClue(file) { file.size.toInt should be > 0 }  // file shouldn't be empty and shouldn't be larger than 2GB
      }
    }

    if (settings.publishing.downloadImages) it("returns some image files") {
      atLeast(10, fileNames) should (endWith(".gif") or endWith(".jpg") or endWith(".jpeg") or endWith(".png"))
    }

    it("generates some NITF files") {
      atLeast(settings.publishing.minArticlesPerEdition, fileNames) should endWith(fileSettings.nitfExtension)
    }

    it("generates valid NITF files") {
      forEvery(nitfFiles) { file => withClue(file) {
        val bareNitf = XML.loadFile(file.toJava)
        val nitf = ArticleNITF.qualify(bareNitf)  // specify the `xmlns` to validate against
        validateXml(nitf, Resources.NitfSchemasContents)
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
      val rootManifestPath = filesEndingWith(fileSettings.rootManifestFileName).head

      val linkedManifests = linkedFiles(rootManifestPath)
      val otherManifests = rssFiles.filterNot(_ == rootManifestPath)

      assertLinkedFilesCoverAllLinkableFiles(linkedManifests, otherManifests)
    }

    def linkedFiles(manifestFile: File) = { withClue(manifestFile) {
      val xml = XML.loadFile(manifestFile.toJava)
      xml.label shouldBe "rss"

      val items = xml \ "channel" \ "item" \ "link"
      items should not be empty

      items.map(_.text.trim).map(manifestFile.sibling)
    }}

    def assertLinkedFilesCoverAllLinkableFiles(linkedFiles: Seq[File], linkables: Seq[File]) = {
      assume(linkables.forall(_.exists()))
      forEvery(linkedFiles) { _.toJava should exist }
      linkedFiles should contain theSameElementsAs linkables
    }
  }
}
