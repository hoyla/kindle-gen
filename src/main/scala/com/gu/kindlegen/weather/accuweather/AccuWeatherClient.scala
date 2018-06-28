package com.gu.kindlegen.weather.accuweather

import java.net.URI

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

import com.softwaremill.sttp._
import com.softwaremill.sttp.json4s._
import org.apache.logging.log4j.scala.Logging
import org.json4s._

import com.gu.io.sttp.SttpDownloader
import com.gu.json4s._
import com.gu.kindlegen.weather.WeatherClient
import com.gu.kindlegen.weather.WeatherClient._


final case class AccuWeatherSettings(apiKey: String, baseUrl: URI)

object AccuWeatherClient {
  def apply(settings: AccuWeatherSettings, downloader: SttpDownloader)(implicit ec: ExecutionContext): AccuWeatherClient = {
    apply(settings.apiKey, settings.baseUrl, downloader)
  }

  def apply(apiKey: String,
            apiBaseUri: URI,
            downloader: SttpDownloader)(implicit ec: ExecutionContext): AccuWeatherClient = {
    new AccuWeatherClient(apiKey, apiBaseUri, knownLocations, downloader)
  }

  lazy val knownLocations: Set[Location] = {
    Source.fromResource("accuweather-cities.csv")
      .getLines
      .drop(1)  // header
      .map(_.split(','))
      .collect {
        case Array(cityName, locationKey, _*) =>
          Location(cityName, locationKey)
      }.toSet
  }

  private[accuweather] implicit val singleForecastReader: Reader[Forecast] = (root: JValue) => {
    val forecast = root.detect(r => (r \ "DailyForecasts") (0)) // first element of the array

    val airQualityIndex = forecast.detectOpt { f =>
      (f \ "AirAndPollen") // returns an array
        .find(_ \ "Name" == JString("AirQuality"))
    }

    val temperature = forecast.detect(_ \ "Temperature")
    val Seq(high, low) = Seq("Maximum", "Minimum").map { property =>
      val value = temperature.detect(_ \ property \ "Value")
      value.detectValue[JDouble](identity).map(_.round.toInt)
        .orElse(value.detectValue[JInt](identity).map(_.toInt))  // json4s.native uses JInt if the decimal dot is missing
    }
    val airQuality = airQualityIndex.detectValue[JString](_ \ "Category")
    val headline = root.detectValue[JString](_ \ "Headline" \ "Category")
    val rain = forecast.detectValue[JInt](_ \ "Day" \ "RainProbability").map(_.toInt)

    Forecast(headline, high, low, airQuality, rain)
  }

  private val ForecastPath = Seq("forecasts", "v1", "daily", "1day")
}

/** A client for AccuWeather's API
  *
  * @param apiBaseUri the URL to the API server, e.g. http://dataservice.accuweather.com
  */
class AccuWeatherClient(apiKey: String,
                        apiBaseUri: URI,
                        knownLocations: Set[Location],
                        downloader: SttpDownloader)(implicit ec: ExecutionContext)
    extends WeatherClient with Logging {

  import AccuWeatherClient._

  def locationKey(cityName: String): Future[Location] = {
    logger.debug(s"Looking up location key for $cityName")
    knownLocations.find(_.name.equalsIgnoreCase(cityName)) match {
      case Some(location) => Future.successful(location)
      case None => Future.failed(new NoSuchElementException(s"Location key for $cityName could not be found!"))
    }
  }

  def forecastFor(location: Location): Future[Forecast] = {
    val url = uri"$apiBaseUri/$ForecastPath/${location.key}.json?apikey=$apiKey&details=true&metric=true&language=en-gb"
    val request = sttp.get(url).response(asJson[JValue])

    logger.debug(s"Requesting forecast for $location...")

    downloader.download(request).flatMap { response =>
      logger.debug(s"Received response forecast for $location.")
      logger.trace(response)

      val forecast = response.as[Forecast]

      if (forecast.nonEmpty) {
        Future.successful(forecast)
      } else {
        val msg = s"AccuWeather response for ${location.name} did not contain a forecast!"
        logger.warn(msg + s" Response was: $response")
        Future.failed(new RuntimeException(msg))
      }
    }
  }
}
