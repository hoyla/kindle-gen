package com.gu.kindlegen

import java.net.URI
import java.nio.file.{Files, Path}
import java.time.{DayOfWeek, LocalTime, ZoneId}

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.ValueReader

import com.gu.config._
import com.gu.contentapi.client.model.v1.TagType
import com.gu.io.Link
import com.gu.io.Link.AbsoluteURL
import com.gu.kindlegen.weather.{WeatherArticleSettings, WeatherSettings}

/** Encapsulates the settings of this application */
final case class Settings(accuWeather: AccuWeatherSettings,
                          contentApi: ContentApiCredentials,
                          articles: GuardianProviderSettings,
                          weather: WeatherSettings,
                          books: BookBindingSettings,
                          publishing: PublishingSettings,
                          run: RunSettings,
                          s3: S3Settings) {
  def withPublishingFiles(files: PublishedFileSettings): Settings =
    copy(publishing = publishing.copy(files = files))
}

final case class AccuWeatherSettings(apiKey: String, baseUrl: URI)

final case class BookBindingSettings(mainSections: Seq[MainSectionTemplate])

final case class ContentApiCredentials(apiKey: String, targetUrl: String)

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

/** @param localHour Run within 60 minutes of this hour
  * @param zone the zone used to interpret ''localHour''
  */
final case class RunSettings(localHour: LocalTime, zone: ZoneId)

final case class S3Settings(bucketName: String, bucketDirectory: String, optionalTmpDirOnDisk: Option[Path]) extends com.gu.io.aws.S3Settings {
  lazy val tmpDirOnDisk: Path = optionalTmpDirOnDisk.getOrElse(Files.createTempDirectory(""))
}

object Settings extends RootConfigReader[Settings] {
  def apply(config: Config): Try[Settings] = {
    for {
      accuWeather <- AccuWeatherSettings.fromParentConfig(config)
      contentApi <- ContentApiCredentials.fromParentConfig(config)
      publishing <- PublishingSettings.fromParentConfig(config)
      articles <- GuardianProviderSettings.fromParentConfig(config)
      weather <- WeatherSettingsReader.fromParentConfig(config)
      books <- BookBindingSettings.fromParentConfig(config)
      run <- RunSettings.fromParentConfig(config)
      s3 <- S3Settings.fromParentConfig(config)
    } yield {
      Settings(accuWeather, contentApi, articles, weather, books, publishing, run, s3)
    }
  }
}

object AccuWeatherSettings extends AbstractConfigReader[AccuWeatherSettings]("accuweather") {
  def apply(config: Config): Try[AccuWeatherSettings] = Try {
    config.as[AccuWeatherSettings]
  }
}

object BookBindingSettings extends AbstractConfigReader[BookBindingSettings]("books") {
  def apply(config: Config): Try[BookBindingSettings] = Try {
    implicit val linkReader = AbsoluteURLReader
    config.as[BookBindingSettings]
  }
}

object ContentApiCredentials extends AbstractConfigReader[ContentApiCredentials]("content-api") {
  def apply(config: Config): Try[ContentApiCredentials] = Try {
    val key    = config.as[String](Key)
    val apiUrl = config.as[String](TargetUrl)
    ContentApiCredentials(key, apiUrl)
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
    config.as[PublishedFileSettings]
  }

  // configuration properties have the same names as the class fields
}

object RunSettings extends AbstractConfigReader[RunSettings]("run") {
  override def apply(config: Config): Try[RunSettings] = Try {
    config.as[RunSettings]
  }
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
  private implicit val linkReader = AbsoluteURLReader

  override def apply(config: Config): Try[WeatherSettings] = Try {
    val minForecastsPercentage = config.as[Int](MinForecastsPercentage)
    val articles = config.as[Seq[WeatherArticleSettings]](Articles)
    val sections = dailySections(config)
    WeatherSettings(minForecastsPercentage, articles, sections)
  }

  private def dailySections(config: Config): Map[DayOfWeek, Section] = {
    val sections = config.as[Map[String, Section]](Sections)
    val defaultSection = sections(DefaultSection)
    sections
      .filterKeys(_ != DefaultSection)
      .map {
        case (dayName, section) => DayOfWeek.valueOf(dayName.toUpperCase) -> section
      }
      .withDefaultValue(defaultSection)
  }

  private final val Articles = "articles"
  private final val DefaultSection = "default"
  private final val MinForecastsPercentage = "minForecastsPercentage"
  private final val Sections = "sections"
}

private object AbsoluteURLReader extends ValueReader[Link] {
  override def read(config: Config, path: String): Link = AbsoluteURL.from(config.as[String](path))
}
