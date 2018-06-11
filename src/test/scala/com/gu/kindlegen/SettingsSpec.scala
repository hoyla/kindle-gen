package com.gu.kindlegen

import java.nio.file.Paths
import java.time.Duration

import scala.collection.JavaConverters._

import com.typesafe.config._
import org.scalatest.FunSpec
import org.scalatest.Matchers._

import com.gu.contentapi.client.model.v1.TagType
import com.gu.scalatest.PathMatchers._


object SettingsSpec {
  implicit class RichMap(val values: Map[String, _]) extends AnyVal {
    def toConfig: Config = toConfigObj.toConfig
    def toConfigObj: ConfigObject = ConfigValueFactory.fromMap(values.asJava)
  }
}

class SettingsSpec extends FunSpec {
  import SettingsSpec._

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
    "files" -> publishedFilesValues.toConfigObj,
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

  private def settingsConfig = Map(
    "content-api" -> contentApiConfig,
    "publishing" -> publishingConfig,
    "gu-capi" -> capiConfig,
    "s3" -> s3Config,
  ).toConfig
  private def contentApiConfig = contentApiValues.toConfigObj
  private def publishingConfig = publishingValues.toConfigObj
  private def capiConfig = capiValues.toConfigObj
  private def s3Config = s3Values.toConfigObj

  describe("Settings factory") {
    val settings = Settings(settingsConfig).get

    it("parses ContentApiSettings correctly") {
      val contentApiSettings = settings.contentApi

      contentApiSettings.apiKey shouldBe contentApiValues("key")
      contentApiSettings.targetUrl shouldBe contentApiValues("url")
    }

    it("parses PublishingSettings correctly") {
      val publishingSettings = settings.publishing

      publishingSettings.downloadImages shouldBe true
      publishingSettings.prettifyXml shouldBe true
      publishingSettings.minArticlesPerEdition shouldBe publishingValues("minArticlesPerEdition")
      publishingSettings.publicationName shouldBe publishingValues("publicationName")
      publishingSettings.publicationLink shouldBe publishingValues("publicationLink")
    }

    it("parses PublishedFileSettings correctly") {
      val fileSettings = settings.publishing.files

      fileSettings.outputDir should beTheSameFileAs(Paths.get(publishedFilesValues("outputDir")))
      fileSettings.nitfExtension shouldBe publishedFilesValues("nitfExtension")
      fileSettings.rssExtension shouldBe publishedFilesValues("rssExtension")
      fileSettings.rootManifestFileName shouldBe publishedFilesValues("rootManifestFileName")
    }

    it("parses GuardianProviderSettings correctly") {
      val providerSettings = settings.provider

      Duration.ofNanos(providerSettings.downloadTimeout.toNanos) shouldBe capiValues("downloadTimeout")
      providerSettings.maxImageResolution shouldBe capiValues("maxImageResolution")
      providerSettings.sectionTagType shouldBe sectionTagType
    }

    it("parses S3Settings correctly") {
      val s3Settings = settings.s3

      s3Settings.bucketName shouldBe s3Values("bucket")
      s3Settings.bucketDirectory shouldBe s3Values("prefix")
      s3Settings.tmpDirOnDisk.toString shouldBe s3Values("tmpDirOnDisk")
    }
  }
}
