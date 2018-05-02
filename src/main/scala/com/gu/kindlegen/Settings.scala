package com.gu.kindlegen

import java.nio.file.{Path, Paths}

import scala.concurrent.duration.Duration
import scala.util.Try

import com.typesafe.config.{Config, ConfigFactory}

import com.gu.contentapi.client.model.v1.TagType

/** Encapsulates the settings of this application */
final case class Settings(contentApi: ContentApiSettings, publishing: PublishingSettings, query: QuerySettings)

final case class ContentApiSettings(apiKey: String, targetUrl: String)

final case class PublishedFileSettings(outputDir: Path,
                                       nitfExtension: String,
                                       rssExtension: String,
                                       rootManifestFileName: String) {
  def encoding = "UTF-8"
}

final case class PublishingSettings(minArticlesPerEdition: Int,
                                    downloadImages: Boolean,
                                    prettifyXml: Boolean,
                                    publicationName: String,
                                    publicationLink: String,
                                    files: PublishedFileSettings)

final case class QuerySettings(downloadTimeout: Duration, sectionTagType: TagType)

object Settings {
  def load: Try[Settings] = {
    apply(ConfigFactory.load)
  }

  def apply(config: Config): Try[Settings] = {
    for {
      contentApi <- ContentApiSettings.fromParentConfig(config)
      publishing <- PublishingSettings.fromParentConfig(config)
      query <- QuerySettings.fromParentConfig(config)
    } yield {
      Settings(contentApi, publishing, query)
    }
  }
}

abstract class SettingsFactory[T](parentConfigPath: String) {
  def fromParentConfig(root: Config): Try[T] =
    apply(root.getConfig(parentConfigPath))

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
      prettifyXml <- Try(config.getBoolean(PrettifyXml))
      minArticles <- Try(config.getInt(MinArticlesPerEdition))
      publicationName <- Try(config.getString(PublicationName))
      publicationLink <- Try(config.getString(PublicationLink))
      fileSettings <- PublishedFileSettings.fromParentConfig(config)
    } yield {
      PublishingSettings(minArticles, downloadImages, prettifyXml, publicationName, publicationLink, fileSettings)
    }
  }

  private final val DownloadImages = "images.download"
  private final val PrettifyXml = "prettifyXml"
  private final val MinArticlesPerEdition = "minArticlesPerEdition"
  private final val PublicationName = "publicationName"
  private final val PublicationLink = "publicationLink"
}

object PublishedFileSettings extends SettingsFactory[PublishedFileSettings]("files") {
  def apply(config: Config): Try[PublishedFileSettings] = {
    for {
      outputDir <- Try(config.getString(OutputDir)).map(Paths.get(_))
      nitfExtension <- Try(config.getString(NitfExtension).stripPrefix("."))
      rssExtension <- Try(config.getString(RssExtension).stripPrefix("."))
      rootManifest <- Try(config.getString(RootManifest)).map(Paths.get(_).getFileName.toString)
    } yield {
      PublishedFileSettings(outputDir, nitfExtension = nitfExtension, rssExtension = rssExtension, rootManifest)
    }
  }

  private final val OutputDir = "outputDir"
  private final val NitfExtension = "nitfExtension"
  private final val RssExtension = "rssExtension"
  private final val RootManifest = "rootManifestFileName"
}

object QuerySettings extends SettingsFactory[QuerySettings]("query") {
  def apply(config: Config): Try[QuerySettings] = {
    for {
      downloadDuration <- Try(config.getDuration(DownloadDuration)).map(javaDuration => Duration.fromNanos(javaDuration.toNanos))
      sectionTagTypeName <- Try(config.getString(SectionTagType))
      sectionTagType <- Try(TagType.valueOf(sectionTagTypeName).get)
    } yield {
      QuerySettings(downloadDuration, sectionTagType)
    }
  }

  private final val DownloadDuration = "downloadTimeout"
  private final val SectionTagType = "sectionTagType"
}
