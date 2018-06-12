package com.gu.kindlegen

import java.nio.file.{Files, Path}

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader

import com.gu.config._
import com.gu.contentapi.client.model.v1.TagType
import com.gu.io.Link
import com.gu.io.Link.AbsoluteURL
import com.gu.kindlegen.weather.WeatherSettings

/** Encapsulates the settings of this application */
final case class Settings(contentApi: ContentApiSettings,
                          articles: GuardianProviderSettings,
                          weather: WeatherSettings,
                          publishing: PublishingSettings,
                          s3: S3Settings) {
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

final case class GuardianProviderSettings(downloadTimeout: FiniteDuration,
                                          sectionTagType: TagType,
                                          maxImageResolution: Int)

final case class S3Settings(bucketName: String, bucketDirectory: String, optionalTmpDirOnDisk: Option[Path]) extends com.gu.io.aws.S3Settings {
  lazy val tmpDirOnDisk: Path = optionalTmpDirOnDisk.getOrElse(Files.createTempDirectory(""))
}

object Settings extends RootConfigReader[Settings] {
  def apply(config: Config): Try[Settings] = {
    for {
      contentApi <- ContentApiSettings.fromParentConfig(config)
      publishing <- PublishingSettings.fromParentConfig(config)
      articles <- GuardianProviderSettings.fromParentConfig(config)
      weather <- WeatherSettingsReader.fromParentConfig(config)
      s3 <- S3Settings.fromParentConfig(config)
    } yield {
      Settings(contentApi, articles, weather, publishing, s3)
    }
  }
}

object ContentApiSettings extends AbstractConfigReader[ContentApiSettings]("content-api") {
  def apply(config: Config): Try[ContentApiSettings] = Try {
    val key    = config.as[String](Key)
    val apiUrl = config.as[String](TargetUrl)
    ContentApiSettings(key, apiUrl)
  }

  private final val Key = "key"
  private final val TargetUrl = "url"
}

object GuardianProviderSettings extends AbstractConfigReader[GuardianProviderSettings]("gu-capi") {
  def apply(config: Config): Try[GuardianProviderSettings] = Try {
    val downloadDuration   = config.as[FiniteDuration](DownloadDuration)
    val maxImageResolution = config.as[Int](MaxImageResolution)
    val sectionTagTypeName = config.as[String](SectionTagType)
    val sectionTagType     = TagType.valueOf(sectionTagTypeName).get
    GuardianProviderSettings(downloadDuration, sectionTagType, maxImageResolution)
  }

  private final val DownloadDuration = "downloadTimeout"
  private final val MaxImageResolution = "maxImageResolution"
  private final val SectionTagType = "sectionTagType"
}

object PublishingSettings extends AbstractConfigReader[PublishingSettings]("publishing") {
  def apply(config: Config): Try[PublishingSettings] = {
    PublishedFileSettings.fromParentConfig(config).map { fileSettings =>
      val downloadImages  = config.as[Boolean](DownloadImages)
      val prettifyXml     = config.as[Boolean](PrettifyXml)
      val minArticles     = config.as[Int](MinArticlesPerEdition)
      val publicationName = config.as[String](PublicationName)
      val publicationLink = config.as[String](PublicationLink)
      PublishingSettings(minArticles, downloadImages, prettifyXml, publicationName, publicationLink, fileSettings)
    }
  }

  private final val DownloadImages = "images.download"
  private final val PrettifyXml = "prettifyXml"
  private final val MinArticlesPerEdition = "minArticlesPerEdition"
  private final val PublicationName = "publicationName"
  private final val PublicationLink = "publicationLink"
}

object PublishedFileSettings extends AbstractConfigReader[PublishedFileSettings]("files") {
  def apply(config: Config): Try[PublishedFileSettings] = Try {
    import net.ceedubs.ficus.readers.ArbitraryTypeReader._
    config.as[PublishedFileSettings]
  }

  // configuration properties have the same names as the class fields
}

object S3Settings extends AbstractConfigReader[S3Settings]("s3") {
  override def apply(config: Config): Try[S3Settings] = Try {
    val bucketName      = config.as[String](BucketName)
    val bucketDirectory = config.as[String](BucketDirectory).stripSuffix("/")
    val tmpDirOnDisk    = config.as[Option[Path]](TmpDirOnDisk)
    S3Settings(bucketName, bucketDirectory, tmpDirOnDisk)
  }

  private final val BucketName = "bucket"
  private final val BucketDirectory = "prefix"
  private final val TmpDirOnDisk = "tmpDirOnDisk"
}

object WeatherSettingsReader extends AbstractConfigReader[WeatherSettings]("weather") {
  private implicit val linkReader = new ValueReader[Link] {
    override def read(config: Config, path: String): Link = AbsoluteURL.from(config.as[String](path))
  }

  override def apply(config: Config): Try[WeatherSettings] = Try {
    import net.ceedubs.ficus.readers.ArbitraryTypeReader._
    config.as[WeatherSettings]
  }

  // configuration properties have the same names as the class fields
}
