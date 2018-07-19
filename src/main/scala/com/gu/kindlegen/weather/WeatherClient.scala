package com.gu.kindlegen.weather

import scala.concurrent.Future


trait WeatherClient {
  import WeatherClient._

  def locationKey(cityName: String): Future[Location]

  /** Retrieves a single-day forecast for the given location */
  def forecastFor(location: Location): Future[Forecast]
}

object WeatherClient {
  /** Unique identifier of a location */
  final case class Location(name: String, key: String, aliases: Set[String] = Set.empty)

  /** A row in our forecasts table */
  case class Forecast(headline: Option[String],
                      high: Option[Int],
                      low: Option[Int],
                      airQuality: Option[String],
                      rainProbability: Option[Int]) {

    val values: Iterable[Option[_]] = this.productIterator.map(_.asInstanceOf[Option[_]]).toIterable

    /** Returns true if at least one field has a value */
    def nonEmpty: Boolean = !isEmpty

    /** Returns true if all fields are empty */
    def isEmpty: Boolean = values.forall(_.isEmpty)
  }
}
