package com.gu.kindlegen.weather

import scala.concurrent.Future
import scala.xml.{Elem, XML}

import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.Inspectors._
import org.scalatest.Matchers._

import com.gu.concurrent.SideEffectsExecutionContext
import com.gu.io.Link.AbsoluteURL
import com.gu.kindlegen._
import com.gu.kindlegen.weather.DailyWeatherForecastProvider.CityForecast
import com.gu.kindlegen.weather.WeatherClient.{Forecast, Location}


class DailyWeatherForecastProviderSpec extends FunSpec {
  private val forecasts = Map(
    Location("London", "GB-LND")  -> Forecast(Some("warm"), Some(25), Some(12), Some("Good"), Some(15)),
    Location("Cairo", "EG-CAI")   -> Forecast(Some("heat"), Some(34), Some(22), Some("Bad"), Some(0)),
    Location("Istanbul", "TR-34") -> Forecast(Some("nice"), Some(24), Some(19), Some("Moderate"), Some(0)),
  )
  private val forecastsByKey = forecasts.map { case (location, forecast) => location.key -> forecast }
  private val forecastsByCity = forecasts.map { case (location, forecast) => location.name -> forecast }
  private val citiesWithForecasts = forecasts.keySet.map(_.name)
  private val locationsWithForecasts = forecasts.keySet
  private val locationWithoutForecast = Location("Dublin", "IE-D")

  private val client = new WeatherClient {
    private val allLocations = locationsWithForecasts + locationWithoutForecast
    override def locationKey(cityName: String): Future[Location] = toFuture(allLocations.find(_.name == cityName))
    override def forecastFor(location: Location): Future[Forecast] = toFuture(forecastsByKey.get(location.key))
    private def toFuture[T](value: Option[T]): Future[T] =
      value.map(Future.successful).getOrElse(Future.failed(new NoSuchElementException))
  }

  private val articleTitle = "UK & Ireland Weather"
  private val articleAbstract = Some("Arbitrary text")
  private val byline = "Powered by AccuWeather™"
  private val section = Section("theguardian/mainsection/weather2", "Weather",
    AbsoluteURL.from("https://www.theguardian.com/theguardian/mainsection/weather2"))

  private val defaultArticleSettings = articleSettingsWithCities(locationsWithForecasts)
  private val defaultSettings = settingsWithArticles(defaultArticleSettings)

  private def articleSettingsWithCities(cities: Iterable[Location]) =
    WeatherArticleSettings(articleTitle, byline, articleAbstract, pageNumber = 0, cities = cities.map(_.name).toSeq, image = None)
  private def settingsWithArticles(articles: WeatherArticleSettings*) =
    WeatherSettings(section, minForecastsPercentage = 50, articles)

  private def provider(settings: WeatherSettings) = {
    implicit val ec = SideEffectsExecutionContext  // discard errors reported by Future#andThen and FutureUtils.successfulSequence
    new DailyWeatherForecastProvider(client, section, settings = settings)
  }

  it("creates an article with a forecast for each city") {
    checkGeneratedArticles(defaultSettings)
  }

  it("creates as many articles as required") {
    val articleSettings = citiesWithForecasts.toSeq.zipWithIndex.map { case (city, i) =>
      WeatherArticleSettings(s"Article $i", s"Byline $i", Some(s"Abstract $i"), pageNumber = i, cities = Seq(city), image = None)
    }
    val settings = settingsWithArticles(articleSettings: _*)
    checkGeneratedArticles(settings)
  }

  it("ignores failed city forecasts") {
    checkGeneratedArticles(defaultSettings, missingCities = Set(locationWithoutForecast.name))
  }

  it("ignores failed city lookups") {
    checkGeneratedArticles(defaultSettings, missingCities = Set("Unknown"))
  }

  it("succeeds with no articles if there aren't enough forecasts") {  // because it shouldn't stop the generation of the issue
    val missingCities = (1 to 4).map(i => Location(i.toString, ""))
    val settings = settingsWithArticles(articleSettingsWithCities(locationsWithForecasts ++ missingCities))
    val articles = provider(settings).fetchArticles().futureValue
    articles shouldBe empty
  }

  private def checkGeneratedArticles(settings: WeatherSettings,
                                     missingCities: Set[String] = Set.empty) = {
    val articles = provider(settings).fetchArticles().futureValue  // future should be successful
    articles should have size settings.articles.size

    forEvery(articles.zip(settings.articles)) { case (article, articleSettings) =>
      article.title shouldBe articleSettings.title
      article.byline shouldBe articleSettings.byline
      article.mainImage shouldBe articleSettings.image
      article.newspaperPageNumber shouldBe articleSettings.pageNumber
      article.articleAbstract shouldBe articleSettings.articleAbstract.getOrElse("")

      val xhtml = XML.loadString(article.bodyBlocks.mkString)
      checkArticleContents(xhtml, articleSettings)
    }
  }

  private def checkArticleContents(xhtml: Elem, articleSettings: WeatherArticleSettings) = {
    val testableCities = articleSettings.cities.filter(citiesWithForecasts)
    val testableForecasts = testableCities.map(city => CityForecast(city, forecastsByCity(city)))

    val headers = xhtml \\ "thead" \\ "td"
    val dummyForecast = Forecast(None, None, None, None, None)
    headers should have size (dummyForecast.productArity + 1)  // 1 for the city column

    val rows = xhtml \\ "tbody" \ "tr"
    rows should have size testableForecasts.size

    forEvery(testableForecasts.zip(rows)) { case (CityForecast(cityName, forecast), row) =>
      val values = (row \ "td").map(_.text)
      values should contain theSameElementsAs (Seq(cityName) ++ forecast.values.map(_.get.toString))
    }
  }
}
