package com.gu.kindlegen

import java.nio.file.{Files, Path}

import scala.concurrent.duration.Duration
import scala.util.Try

import com.typesafe.config.Config

import com.gu.config._
import com.gu.contentapi.client.model.v1.TagType

/** Encapsulates the settings of this application */
final case class Settings(contentApi: ContentApiSettings, publishing: PublishingSettings, query: QuerySettings, s3: S3Settings) {
  def withPublishingFiles(files: PublishedFileSettings): Settings =
    copy(publishing = publishing.copy(files = files))
}

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

final case class QuerySettings(downloadTimeout: Duration, sectionTagType: TagType, maxImageResolution: Int)

case class S3Settings(bucketName: String, bucketDirectory: String, tmpDirOnDisk: Path) extends com.gu.io.aws.S3Settings

object Settings extends RootSettingsFactory[Settings] {
  def apply(config: Config): Try[Settings] = {
    for {
      contentApi <- ContentApiSettings.fromParentConfig(config)
      publishing <- PublishingSettings.fromParentConfig(config)
      query <- QuerySettings.fromParentConfig(config)
      s3 <- S3Settings.fromParentConfig(config)
    } yield {
      Settings(contentApi, publishing, query, s3)
    }
  }
}

object ContentApiSettings extends AbstractSettingsFactory[ContentApiSettings]("content-api") {
  def apply(config: Config): Try[ContentApiSettings] = Try {
    val key    = config.getString(Key)
    val apiUrl = config.getString(TargetUrl)
    ContentApiSettings(key, apiUrl)
  }

  private final val Key = "key"
  private final val TargetUrl = "url"
}

object PublishingSettings extends AbstractSettingsFactory[PublishingSettings]("publishing") {
  def apply(config: Config): Try[PublishingSettings] = {
    PublishedFileSettings.fromParentConfig(config).map { fileSettings =>
      val downloadImages  = config.getBoolean(DownloadImages)
      val prettifyXml     = config.getBoolean(PrettifyXml)
      val minArticles     = config.getInt(MinArticlesPerEdition)
      val publicationName = config.getString(PublicationName)
      val publicationLink = config.getString(PublicationLink)
      PublishingSettings(minArticles, downloadImages, prettifyXml, publicationName, publicationLink, fileSettings)
    }
  }

  private final val DownloadImages = "images.download"
  private final val PrettifyXml = "prettifyXml"
  private final val MinArticlesPerEdition = "minArticlesPerEdition"
  private final val PublicationName = "publicationName"
  private final val PublicationLink = "publicationLink"
}

object PublishedFileSettings extends AbstractSettingsFactory[PublishedFileSettings]("files") {
  def apply(config: Config): Try[PublishedFileSettings] = Try {
    val outputDir     = config.getPath(OutputDir)
    val nitfExtension = config.getString(NitfExtension).stripPrefix(".")
    val rssExtension  = config.getString(RssExtension).stripPrefix(".")
    val rootManifest  = config.getPath(RootManifest).getFileName.toString
    PublishedFileSettings(outputDir, nitfExtension = nitfExtension, rssExtension = rssExtension, rootManifest)
  }

  private final val OutputDir = "outputDir"
  private final val NitfExtension = "nitfExtension"
  private final val RssExtension = "rssExtension"
  private final val RootManifest = "rootManifestFileName"
}

object QuerySettings extends AbstractSettingsFactory[QuerySettings]("query") {
  def apply(config: Config): Try[QuerySettings] = Try {
    val downloadDuration   = config.getFiniteDuration(DownloadDuration)
    val maxImageResolution = config.getInt(MaxImageResolution)
    val sectionTagTypeName = config.getString(SectionTagType)
    val sectionTagType     = TagType.valueOf(sectionTagTypeName).get
    QuerySettings(downloadDuration, sectionTagType, maxImageResolution)
  }

  private final val DownloadDuration = "downloadTimeout"
  private final val MaxImageResolution = "maxImageResolution"
  private final val SectionTagType = "sectionTagType"
}

object S3Settings extends AbstractSettingsFactory[S3Settings]("s3") {
  override def apply(config: Config): Try[S3Settings] = Try {
    val bucketName = config.getString(BucketName)
    val bucketDirectory = config.getString(BucketDirectory).stripSuffix("/")
    val tmpDirOnDisk = Try(config.getPath(TmpDirOnDisk)).getOrElse(Files.createTempDirectory(""))
    S3Settings(bucketName, bucketDirectory, tmpDirOnDisk)
  }

  private final val BucketName = "bucket"
  private final val BucketDirectory = "prefix"
  private final val TmpDirOnDisk = "tmpDirOnDisk"
}
