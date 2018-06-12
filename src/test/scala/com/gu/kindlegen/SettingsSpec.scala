package com.gu.kindlegen

import java.nio.file.Paths
import java.time.Duration

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

import com.typesafe.config._
import org.scalatest.{Assertion, FunSpec}
import org.scalatest.Matchers._

import com.gu.config.ConfigReader
import com.gu.contentapi.client.model.v1.TagType
import com.gu.scalatest.PathMatchers._


class SettingsSpec extends FunSpec {
  import SettingsSpec._

  testReader(ContentApiSettings, contentApiConfig)(validateValues)

  testReader(PublishedFileSettings, publishedFilesConfig)(validateValues)

  testReader(PublishingSettings, publishingConfig)(validateValues)

  testReader(GuardianProviderSettings, capiConfig)(validateValues)

  testReader(S3Settings, s3Config)(validateValues)

  testReader(Settings, settingsConfig)(validateValues)

  describe("Settings.load") {
    it("reads the configuration file correctly") {
      validateSettings(Settings.load) { _: Settings => /* just test that it is read without errors */ }
    }
  }

  private def testReader[T](reader: ConfigReader[T], sampleConfig: ConfigObject)(validator: T => Assertion) = {
    describe(reader.getClass.getSimpleName) {
      it("reads a sample configuration correctly") {
        validateSettings(reader(sampleConfig.toConfig))(validator)
      }
    }
  }

  private def validateSettings[T](maybeSettings: Try[T])(validate: T => _) = {
    maybeSettings match {
      case Success(settings) => validate(settings)
      case Failure(error) => fail("Failed to read configuration", error)
    }
  }
}

object SettingsSpec {
  implicit class RichMap(val values: Map[String, _]) extends AnyVal {
    def toConfigObj: ConfigObject = ConfigValueFactory.fromMap(values.asJava)
  }

  private val contentApiValues = Map(
    "key" -> "My API key",
    "url" -> "https://example.com"
  )

  private val publishedFilesValues = Map(
    "outputDir" -> "/home/me",
    "nitfExtension" -> "nitf",
    "rssExtension" -> "rss",
    "rootManifestFileName" -> "something.xml"
  )

  private val publishingValues = Map(
    "minArticlesPerEdition" -> 10,
    "publicationName" -> "My Publication",
    "publicationLink" -> "http://example.com",
    "prettifyXml" -> "on",
    "files" -> publishedFilesConfig,
    "images" -> Map("download" -> "on").toConfigObj
  )

  private val sectionTagType = TagType.Keyword
  private val capiValues = Map(
    "downloadTimeout" -> Duration.ofSeconds(30),
    "maxImageResolution" -> 500,
    "sectionTagType" -> sectionTagType.name
  )

  private val s3Values = Map(
    "bucket" -> "My Bucket",
    "prefix" -> "A_Prefix",
    "tmpDirOnDisk" -> "/tmp"
  )

  private val settingsValues = Map(
    "content-api" -> contentApiConfig,
    "publishing" -> publishingConfig,
    "gu-capi" -> capiConfig,
    "s3" -> s3Config,
  )

  private def settingsConfig = settingsValues.toConfigObj
  private def contentApiConfig = contentApiValues.toConfigObj
  private def publishingConfig = publishingValues.toConfigObj
  private def publishedFilesConfig = publishedFilesValues.toConfigObj
  private def capiConfig = capiValues.toConfigObj
  private def s3Config = s3Values.toConfigObj

  private def validateValues(settings: Settings): Assertion = {
    validateValues(settings.contentApi)
    validateValues(settings.provider)
    validateValues(settings.publishing)
    validateValues(settings.s3)
  }

  private def validateValues(contentApiSettings: ContentApiSettings): Assertion = {
    contentApiSettings.apiKey shouldBe contentApiValues("key")
    contentApiSettings.targetUrl shouldBe contentApiValues("url")
  }

  private def validateValues(publishingSettings: PublishingSettings): Assertion = {
    publishingSettings.downloadImages shouldBe true
    publishingSettings.prettifyXml shouldBe true
    publishingSettings.minArticlesPerEdition shouldBe publishingValues("minArticlesPerEdition")
    publishingSettings.publicationName shouldBe publishingValues("publicationName")
    publishingSettings.publicationLink shouldBe publishingValues("publicationLink")

    validateValues(publishingSettings.files)
  }

  private def validateValues(fileSettings: PublishedFileSettings): Assertion = {
    fileSettings.outputDir should beTheSameFileAs(Paths.get(publishedFilesValues("outputDir")))
    fileSettings.nitfExtension shouldBe publishedFilesValues("nitfExtension")
    fileSettings.rssExtension shouldBe publishedFilesValues("rssExtension")
    fileSettings.rootManifestFileName shouldBe publishedFilesValues("rootManifestFileName")
  }

  private def validateValues(providerSettings: GuardianProviderSettings): Assertion = {
    Duration.ofNanos(providerSettings.downloadTimeout.toNanos) shouldBe capiValues("downloadTimeout")
    providerSettings.maxImageResolution shouldBe capiValues("maxImageResolution")
    providerSettings.sectionTagType shouldBe sectionTagType
  }

  private def validateValues(s3Settings: S3Settings): Assertion = {
    s3Settings.bucketName shouldBe s3Values("bucket")
    s3Settings.bucketDirectory shouldBe s3Values("prefix")
    s3Settings.tmpDirOnDisk.toString shouldBe s3Values("tmpDirOnDisk")
  }
}
