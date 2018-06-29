package com.gu.kindlegen.app

import java.nio.file.Path
import java.time.DayOfWeek

import scala.util.Try

import com.typesafe.config.Config
import com.typesafe.config.ConfigException.BadValue
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import com.gu.config._
import com.gu.contentapi.client.model.v1.{Tag, TagType}
import com.gu.io.Link
import com.gu.io.Link.AbsoluteURL
import com.gu.kindlegen._
import com.gu.kindlegen.accuweather.AccuWeatherSettings
import com.gu.kindlegen.capi.{ContentApiCredentials, GuardianProviderSettings}
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
  private implicit val tagReader = TagReader
  private implicit val tagTypeReader = TagTypeReader

  val accuWeatherSettingsReader      = ConfigReader[AccuWeatherSettings]("accuweather")
  val bookBindingSettingsReader      = ConfigReader[BookBindingSettings]("books")
  val contentApiCredentialsReader    = ConfigReader[ContentApiCredentials]("content-api")
  val guardianProviderSettingsReader = ConfigReader[GuardianProviderSettings]("gu-capi")
  val publishingSettingsReader       = ConfigReader[PublishingSettings]("publishing")
  val runSettingsReader              = ConfigReader[RunSettings]("run")
  val s3SettingsReader               = ConfigReader[S3Settings]("s3")
  val weatherSettingsReader          = ConfigReader[WeatherSettings]("weather")
}

private object AbsoluteURLReader extends ValueReader[Link] {
  override def read(config: Config, path: String): Link = AbsoluteURL.from(config.as[String](path))
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

object TagReader extends ValueReader[Tag] {
  override def read(parentConfig: Config, path: String): Tag = {
    implicit val tagTypeReader = TagTypeReader
    val config = parentConfig.as[Config](path)

    val id = config.as[String](Id)
    val tagType = config.as[TagType](Type)
    Tag(id, tagType, apiUrl = "", webUrl = "", webTitle = "")
  }

  private final val Id = "id"
  private final val Type = "type"
}

object TagTypeReader extends ValueReader[TagType] {
  override def read(config: Config, path: String): TagType = {
    val tagTypeName = config.as[String](path)

    TagType.valueOf(tagTypeName)
      .getOrElse(throw new BadValue(config.origin(), path,
        s"$tagTypeName isn't a valid value for c.g.c.c.m.v1.TagType; allowed values: ${TagType.list.mkString(", ")}"))
  }
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
