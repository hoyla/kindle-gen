package com.gu.kindlegen

import java.nio.file.Paths

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

  private val publishingValues = Map(
    "minArticlesPerEdition" -> 10,
    "publicationName" -> "My Publication",
    "publicationLink" -> "http://example.com",
    "outputDir" -> "/home/me",
    "images" -> Map("download" -> "on").toConfigObj
  )

  private val sectionTagType = TagType.Keyword
  private val queryValues = Map(
    "sectionTagType" -> sectionTagType.name
  )

  private def settingsConfig = Map(
    "content-api" -> contentApiConfig,
    "publishing" -> publishingConfig,
    "query" -> queryConfig,
  ).toConfig
  private def contentApiConfig = contentApiValues.toConfigObj
  private def publishingConfig = publishingValues.toConfigObj
  private def queryConfig = queryValues.toConfigObj

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
      publishingSettings.minArticlesPerEdition shouldBe publishingValues("minArticlesPerEdition")
      publishingSettings.publicationName shouldBe publishingValues("publicationName")
      publishingSettings.publicationLink shouldBe publishingValues("publicationLink")

      publishingSettings.outputDir should beTheSameFileAs(Paths.get(publishingValues("outputDir").toString))
    }

    it("parses QuerySettings correctly") {
      val querySettings = settings.query

      querySettings.sectionTagType shouldBe sectionTagType
    }
  }
}
