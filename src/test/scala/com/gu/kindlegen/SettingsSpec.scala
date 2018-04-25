package com.gu.kindlegen

import java.nio.file.{Files, Paths}

import scala.collection.JavaConverters._

import com.typesafe.config._
import org.scalatest.FunSpec
import org.scalatest.Matchers._
import com.gu.scalatest.PathMatchers._


object SettingsSpec {
  implicit class RichMap(val values: Map[String, _]) extends AnyVal {
    def toConfig: Config = toConfigObj.toConfig
    def toConfigObj: ConfigObject = ConfigValueFactory.fromMap(values.asJava)
  }
}

class SettingsSpec extends FunSpec {
  import SettingsSpec._

  val contentApiValues = Map(
    "key" -> "My API key",
    "url" -> "https://example.com"
  )
  val publishingValues = Map(
    "minArticlesPerEdition" -> 10,
    "publicationName" -> "My Publication",
    "outputDir" -> "/home/me",
    "images" -> Map("download" -> "on").toConfigObj
  )

  private def settingsConfig = Map(
    "content-api" -> contentApiConfig,
    "publishing" -> publishingConfig
  ).toConfig
  private def contentApiConfig = contentApiValues.toConfigObj
  private def publishingConfig = publishingValues.toConfigObj

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

      publishingSettings.outputDir should beTheSameFileAs(Paths.get(publishingValues("outputDir").toString))
    }
  }
}
