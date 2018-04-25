package com.gu.kindlegen

import java.nio.file.{Path, Paths}

import scala.util.Try

import com.typesafe.config.{Config, ConfigFactory}

/** Encapsulates the settings of this application */
final case class Settings(contentApi: ContentApiSettings, publishing: PublishingSettings)

final case class ContentApiSettings(apiKey: String, targetUrl: String)
final case class PublishingSettings(downloadImages: Boolean, outputDir: Path, publicationName: String)

object Settings {
  def load: Try[Settings] = {
    apply(ConfigFactory.load)
  }

  def apply(config: Config): Try[Settings] = {
    for {
      contentApi <- ContentApiSettings.fromRootConfig(config)
      publishing <- PublishingSettings.fromRootConfig(config)
    } yield {
      Settings(contentApi, publishing)
    }
  }
}

abstract class SettingsFactory[T](rootConfigPath: String) {
  def fromRootConfig(root: Config): Try[T] =
    apply(root.getConfig(rootConfigPath))

  def apply(config: Config): Try[T]
}

object ContentApiSettings extends SettingsFactory[ContentApiSettings]("content-api") {
  def apply(config: Config): Try[ContentApiSettings] = {
    for {
      key <- Try(config.getString(Key))
      apiUrl <- Try(config.getString(TargetUrl))
    } yield {
      ContentApiSettings(key, apiUrl)
    }
  }

  private final val Key = "key"
  private final val TargetUrl = "url"
}

object PublishingSettings extends SettingsFactory[PublishingSettings]("publishing") {
  def apply(config: Config): Try[PublishingSettings] = {
    for {
      downloadImages <- Try(config.getBoolean(DownloadImages))
      outputDir <- Try(config.getString(OutputDir)).map(Paths.get(_))
      publicationName <- Try(config.getString(PublicationName))
    } yield {
      PublishingSettings(downloadImages, outputDir, publicationName)
    }
  }

  private final val DownloadImages = "images.download"
  private final val OutputDir = "outputDir"
  private final val PublicationName = "publicationName"
}
