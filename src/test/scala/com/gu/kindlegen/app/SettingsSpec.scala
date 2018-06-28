package com.gu.kindlegen.app

import java.nio.file.Paths
import java.time.{DayOfWeek, Duration}

import scala.collection.JavaConverters._
import scala.reflect._
import scala.util.{Failure, Success, Try}

import com.typesafe.config._
import org.scalatest.{Assertion, FunSpec}
import org.scalatest.Inspectors._
import org.scalatest.Matchers._

import com.gu.config.ConfigReader
import com.gu.contentapi.client.model.v1.TagType
import com.gu.io.Link.AbsoluteURL
import com.gu.kindlegen._
import com.gu.kindlegen.accuweather.AccuWeatherSettings
import com.gu.kindlegen.capi.{ContentApiCredentials, GuardianProviderSettings}
import com.gu.kindlegen.weather.{WeatherArticleSettings, WeatherSettings}
import com.gu.scalatest.PathMatchers._


class SettingsSpec extends FunSpec {
  import Settings._
  import SettingsSpec._

  testReader(accuWeatherSettingsReader, accuWeatherConfig)(validateValues)

  testReader(bookBindingSettingsReader, bookConfig)(validateValues)

  testReader(contentApiCredentialsReader, contentApiConfig)(validateValues)

  testReader(publishingSettingsReader, publishingConfig)(validateValues)

  testReader(guardianProviderSettingsReader, capiConfig)(validateValues)

  testReader(runSettingsReader, runConfig)(validateValues)

  testReader(s3SettingsReader, s3Config)(validateValues)

  testReader(weatherSettingsReader, weatherConfig)(validateValues)

  testReader(Settings, settingsConfig)(validateValues)

  describe("Settings.load") {
    it("reads the configuration file correctly") {
      validateSettings(Settings.load) { _: Settings => /* just test that it is read without errors */ }
    }
  }

  private def testReader[T: ClassTag](reader: ConfigReader[T], sampleConfig: ConfigObject)(validator: T => Assertion) = {
    describe(classTag[T].runtimeClass.getSimpleName + "Reader") {
      it("reads a sample configuration correctly") {
        validateSettings(reader(sampleConfig.toConfig))(validator)
      }
    }
  }

  private def validateSettings[T](maybeSettings: Try[T])(validate: T => _) = {
    maybeSettings match {
      case Success(settings) => validate(settings)
      case Failure(error) =>
        error.printStackTrace()
        fail(s"Failed to read configuration! $error", error)
    }
  }
}

object SettingsSpec {
  implicit class RichMap(val values: Map[String, _]) extends AnyVal {
    def toConfigObj: ConfigObject = ConfigValueFactory.fromMap(values.asJava)
  }

  private val defaultSection = Section("default-section", "Default Section", AbsoluteURL.from("http://example.com/default"))
  private val specialSection = Section("special", "Special Section", AbsoluteURL.from("http://example.com/special"))
  private val weekendSection = Section("weekend", "Weekend Section", AbsoluteURL.from("http://example.com/weekend"))

  private val accuWeatherValues = Map(
    "apiKey" -> "My weather API key",
    "baseUrl" -> "https://weather.example.com"
  )

  private val contentApiValues = Map(
    "key" -> "My API key",
    "url" -> "https://example.com"
  )

  private val mainSections = Seq(
    toMap(defaultSection),
    Map("id" -> weekendSection.id, "title" -> "Different title", "overrides" -> Seq("x", "y").asJava),
    Map("id" -> specialSection.id),
  )
  private val bookValues = Map(
    "mainSections" -> mainSections.map(_.toConfigObj).asJava
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
    "files" -> publishedFilesConfig,
    "downloadImages" -> "on",
  )

  private val sectionTagType = TagType.Keyword
  private val capiValues = Map(
    "downloadTimeout" -> Duration.ofSeconds(30),
    "maxImageResolution" -> 500,
    "sectionTagType" -> sectionTagType.name
  )

  private val runValues = Map(
    "localHour" -> "06:00",
    "zone" -> "Antarctica/South_Pole"
  )

  private val s3Values = Map(
    "bucket" -> "My Bucket",
    "prefix" -> "A_Prefix",
    "tmpDirOnDisk" -> "/tmp"
  )

  private def weatherImage(country: String) = Map(
    "id" -> country,
    "link" -> s"http://example.com/$country.jpg",
    "altText" -> s"Alternate text for $country",
    "caption" -> s"Caption for $country",
    "credit" -> s"Credit for $country",
  )

  private val weatherArticleValues = Seq("UK", "World", "OuterSpace").zipWithIndex.map { case (country, index) => Map(
    "title" -> s"Weather of the $country",
    "byline" -> country,
    "abstract" -> s"Abstract of the $country",
    "pageNumber" -> index,
    "cities" -> (1 to 3).map(city => s"$country $city").asJava,
    "image" -> weatherImage(country).toConfigObj,
  )}

  private val weatherSections = Map(
    "default" -> defaultSection,
    "saturday" -> weekendSection,
    "Sunday" -> specialSection,
  )
  private val weatherValues = Map(
    "articles" -> weatherArticleValues.map(_.toConfigObj).asJava,
    "minForecastsPercentage" -> 75,
    "sections" -> weatherSections.mapValues(toMap(_).toConfigObj).toConfigObj,
  )
  private def toMap(section: Section) =
    Map("id" -> section.id, "title" -> section.title, "link" -> section.link.source)

  private val settingsValues = Map(
    "accuweather" -> accuWeatherConfig,
    "content-api" -> contentApiConfig,
    "publishing" -> publishingConfig,
    "gu-capi" -> capiConfig,
    "books" -> bookConfig,
    "run" -> runConfig,
    "s3" -> s3Config,
    "weather" -> weatherConfig,
  )

  private def settingsConfig = settingsValues.toConfigObj
  private def accuWeatherConfig = accuWeatherValues.toConfigObj
  private def contentApiConfig = contentApiValues.toConfigObj
  private def publishingConfig = publishingValues.toConfigObj
  private def publishedFilesConfig = publishedFilesValues.toConfigObj
  private def capiConfig = capiValues.toConfigObj
  private def bookConfig = bookValues.toConfigObj
  private def runConfig = runValues.toConfigObj
  private def s3Config = s3Values.toConfigObj
  private def weatherConfig = weatherValues.toConfigObj

  private def validateValues(settings: Settings): Assertion = {
    validateValues(settings.accuWeather)
    validateValues(settings.contentApi)
    validateValues(settings.articles)
    validateValues(settings.publishing)
    validateValues(settings.books)
    validateValues(settings.run)
    validateValues(settings.s3)
    validateValues(settings.weather)
  }

  private def validateValues(accuWeatherSettings: AccuWeatherSettings): Assertion = {
    accuWeatherSettings.apiKey shouldBe accuWeatherValues("apiKey")
    accuWeatherSettings.baseUrl.toString shouldBe accuWeatherValues("baseUrl")
  }

  private def validateValues(contentApiSettings: ContentApiCredentials): Assertion = {
    contentApiSettings.key shouldBe contentApiValues("key")
    contentApiSettings.url shouldBe contentApiValues("url")
  }

  private def validateValues(bookBindingSettings: BookBindingSettings): Assertion = {
    bookBindingSettings.mainSections should have size mainSections.size
    bookBindingSettings.mainSections.map(_.id) should contain theSameElementsInOrderAs mainSections.map(_("id"))
    forEvery(bookBindingSettings.mainSections.filter(_.id == weekendSection.id)) { newWeekendSection =>
      newWeekendSection.title should not equal weekendSection.title
      newWeekendSection.overrides shouldBe Seq("x", "y")
    }
  }

  private def validateValues(publishingSettings: PublishingSettings): Assertion = {
    publishingSettings.downloadImages shouldBe true
    publishingSettings.prettifyXml shouldBe true
    publishingSettings.minArticlesPerEdition shouldBe publishingValues("minArticlesPerEdition")
    publishingSettings.publicationName shouldBe publishingValues("publicationName")
    publishingSettings.publicationLink shouldBe publishingValues("publicationLink")

    validateValues(publishingSettings.files)
  }

  private def validateValues(fileSettings: PublishedFileSettings): Assertion = {
    fileSettings.outputDir should beTheSameFileAs(Paths.get(publishedFilesValues("outputDir")))
    fileSettings.nitfExtension shouldBe publishedFilesValues("nitfExtension")
    fileSettings.rssExtension shouldBe publishedFilesValues("rssExtension")
    fileSettings.rootManifestFileName shouldBe publishedFilesValues("rootManifestFileName")
  }

  private def validateValues(providerSettings: GuardianProviderSettings): Assertion = {
    Duration.ofNanos(providerSettings.downloadTimeout.toNanos) shouldBe capiValues("downloadTimeout")
    providerSettings.maxImageResolution shouldBe capiValues("maxImageResolution")
    providerSettings.sectionTagType shouldBe sectionTagType
  }

  private def validateValues(runSettings: RunSettings): Assertion = {
    runSettings.localHour.toString shouldBe runValues("localHour")
    runSettings.zone.toString shouldBe runValues("zone")
  }

  private def validateValues(s3Settings: S3Settings): Assertion = {
    s3Settings.bucketName shouldBe s3Values("bucket")
    s3Settings.bucketDirectory shouldBe s3Values("prefix")
    s3Settings.tmpDirOnDisk.toString shouldBe s3Values("tmpDirOnDisk")
  }

  private def validateValues(weather: WeatherSettings): Assertion = {
    weather.minForecastsPercentage shouldBe weatherValues("minForecastsPercentage")
    forEvery(weather.articles.zipWithIndex) { case (article, index) =>
      validateValues(article, weatherArticleValues(index))
    }

    val weatherSections = this.weatherSections.map { case (day, section) => day.toUpperCase -> section }
    val sectionKeys = weatherSections.keySet  // day names + "default"

    forEvery(weather.sections) { case (day, section) =>
      section shouldBe weatherSections(day.toString)
    }
    forEvery(DayOfWeek.values.filterNot(day => sectionKeys.contains(day.toString))) { unconfiguredDay =>
      weather.sections(unconfiguredDay) shouldBe defaultSection
    }
  }

  private def validateValues(weatherArticle: WeatherArticleSettings, values: Map[String, _]): Assertion = {
    weatherArticle.articleAbstract shouldBe values.get("abstract")
    weatherArticle.byline shouldBe values("byline")
    weatherArticle.pageNumber shouldBe values("pageNumber")

    val cities = values("cities").asInstanceOf[java.lang.Iterable[String]].asScala
    weatherArticle.cities should contain theSameElementsInOrderAs cities

    val image = values.get("image").map { _ =>
      val v = weatherImage(weatherArticle.byline)
      Image(v("id"), AbsoluteURL.from(v("link")), v.get("altText"), v.get("caption"), v.get("credit"))
    }
    weatherArticle.image shouldBe image
  }
}
