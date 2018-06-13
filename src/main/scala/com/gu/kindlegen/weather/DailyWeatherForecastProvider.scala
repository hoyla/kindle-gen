package com.gu.kindlegen.weather

import java.time.OffsetDateTime

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure
import scala.xml.Elem

import org.apache.logging.log4j.scala.Logging

import com.gu.concurrent.FutureUtils._
import com.gu.kindlegen._
import com.gu.kindlegen.weather.WeatherClient.Forecast


object DailyWeatherForecastProvider {
  protected[weather] final case class CityForecast(cityName: String, forecast: Forecast)
}

class DailyWeatherForecastProvider(client: WeatherClient,
                                   section: Section,
                                   settings: WeatherSettings)(implicit ec: ExecutionContext)
    extends ArticlesProvider with Logging {

  import DailyWeatherForecastProvider._

  protected val minForecastsRatio = settings.minForecastsPercentage / 100.0

  override def fetchArticles(): Future[Seq[Article]] = {
    successfulSequence(  // it's ok to skip some (or all) weather articles
      settings.articles.zipWithIndex
        .map((forecastArticle _).tupled)
    )
  }

  protected def forecastArticle(articleSettings: WeatherArticleSettings, articleNumber: Int): Future[Article] = {
    collectForecasts(articleSettings.cities)
      .map(forecastTable)
      .map { content: Elem =>
        val link = section.link  // the article is generated and has no link of its own; another link will be provided later
        val id = section.id + '_' + articleSettings.title.replace(' ', '-')

        Article(
          section = section,
          newspaperPageNumber = articleSettings.pageNumber,
          title = articleSettings.title,
          docId = id,
          link = link,
          pubDate = OffsetDateTime.now,
          byline = articleSettings.byline,
          articleAbstract = articleSettings.articleAbstract.getOrElse(""),
          bodyBlocks = Seq(content.mkString),
          mainImage = articleSettings.image
        )
      }
  }

  protected def collectForecasts(cityNames: Seq[String]): Future[Seq[CityForecast]] = {
    successfulSequence(cityNames.map(dailyForecast))  // skip failed forecasts
      .flatMap { forecasts =>
        val usableForecasts = forecasts.filter(_.forecast.nonEmpty)
        val minForecasts = (cityNames.size * minForecastsRatio).round

        if (usableForecasts.size >= minForecasts) {
          logger.info(s"Found ${usableForecasts.size} usable forecasts (out of ${cityNames.size}).")
          Future.successful(usableForecasts)
        } else {
          val msg = s"Not enough usable forecasts to generate a weather article!" +
            s" Expected ${cityNames.size}, found $usableForecasts. We need at least $minForecasts."
          logger.warn(msg)
          Future.failed(new RuntimeException(msg))
        }
      }
  }

  protected def dailyForecast(cityName: String): Future[CityForecast] = {
    client.locationKey(cityName)
      .andThen { case Failure(t) => logger.warn(s"Failed to get location key for $cityName!", t) }

      .flatMap {
        client.forecastFor(_)
          .map(CityForecast(cityName, _))
          .andThen { case Failure(t) => logger.warn(s"Failed to get forecast for $cityName!", t) }
      }
  }

  protected def forecastTable(cityForecasts: Seq[CityForecast]) = {
    val tableHeaders = forecastRowFields.map(_._1)
    def forecastRow(forecast: Forecast) = forecastRowFields.map(_._2(forecast).getOrElse(""))

    <table>
      <thead>
        <tr>
          {tableHeaders.map(x => <td>{x}</td>)}
        </tr>
      </thead>
      <tbody>
        {cityForecasts.map { cityForecast =>
          <tr>
            <td>{cityForecast.cityName}</td>
            {forecastRow(cityForecast.forecast).map { value =>
              <td>{value}</td>
            }}
          </tr>
        }}
      </tbody>
    </table>
  }

  protected val forecastRowFields = Seq[(String, Forecast => Option[_])](
    "Hi"      -> { _.high },
    "Lo"      -> { _.low },
    "Rain%"   -> { _.rainProbability },
    "Weather" -> { _.headline },
    "AirQlty" -> { _.airQuality },
  )
}

