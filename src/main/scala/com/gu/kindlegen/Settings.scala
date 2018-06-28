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
import com.gu.kindlegen.weather.WeatherSettings


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

final case class ContentApiCredentials(key: String, url: String)

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
      accuWeather <- accuWeatherSettingsReader.fromParentConfig(config)
      contentApi <- contentApiCredentialsReader.fromParentConfig(config)
      articles <- guardianProviderSettingsReader.fromParentConfig(config)
      weather <- weatherSettingsReader.fromParentConfig(config)
      books <- bookBindingSettingsReader.fromParentConfig(config)
      publishing <- publishingSettingsReader.fromParentConfig(config)
      run <- runSettingsReader.fromParentConfig(config)
      s3 <- s3SettingsReader.fromParentConfig(config)
    } yield {
      Settings(accuWeather, contentApi, articles, weather, books, publishing, run, s3)
    }
  }

  private implicit val dailySectionsReader = WeatherSectionsReader
  private implicit val linkReader = AbsoluteURLReader
  private implicit val s3Reader = S3SettingsReader
  private implicit val tagTypeReader: ValueReader[TagType] =
    (config, path) => TagType.valueOf(config.as[String](path)).get

  val accuWeatherSettingsReader      = ConfigReader[AccuWeatherSettings]("accuweather")
  val bookBindingSettingsReader      = ConfigReader[BookBindingSettings]("books")
  val contentApiCredentialsReader    = ConfigReader[ContentApiCredentials]("content-api")
  val guardianProviderSettingsReader = ConfigReader[GuardianProviderSettings]("gu-capi")
  val publishingSettingsReader       = ConfigReader[PublishingSettings]("publishing")
  val runSettingsReader              = ConfigReader[RunSettings]("run")
  val s3SettingsReader               = ConfigReader[S3Settings]("s3")
  val weatherSettingsReader          = ConfigReader[WeatherSettings]("weather")
}

object S3SettingsReader extends ValueReader[S3Settings] {
  override def read(config: Config, path: String): S3Settings = {
    val bucketName      = config.as[String](BucketName)
    val bucketDirectory = config.as[String](BucketDirectory).stripSuffix("/")
    val tmpDirOnDisk    = config.as[Option[Path]](TmpDirOnDisk)
    S3Settings(bucketName, bucketDirectory, tmpDirOnDisk)
  }

  private final val BucketName = "bucket"
  private final val BucketDirectory = "prefix"
  private final val TmpDirOnDisk = "tmpDirOnDisk"
}

object WeatherSectionsReader extends ValueReader[Map[DayOfWeek, Section]] {
  override def read(config: Config, path: String): Map[DayOfWeek, Section] = {
    implicit val linkReader = AbsoluteURLReader

    val dailySections = config.as[Map[String, Section]](path)
      .map {
        case (dayName, section) => Try(DayOfWeek.valueOf(dayName)).toOption -> section
      }

    val defaultSection = dailySections(None)
    dailySections
      .collect { case (Some(dayOfWeek), section) => dayOfWeek -> section }
      .withDefaultValue(defaultSection)
  }
}

private object AbsoluteURLReader extends ValueReader[Link] {
  override def read(config: Config, path: String): Link = AbsoluteURL.from(config.as[String](path))
}
