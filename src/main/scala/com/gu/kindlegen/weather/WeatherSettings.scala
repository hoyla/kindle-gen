package com.gu.kindlegen.weather

import java.time.DayOfWeek

import com.gu.kindlegen.{Image, Section}


final case class WeatherSettings(minForecastsPercentage: Int,  // how many successful retrievals of forecasts to create an article
                                 articles: Seq[WeatherArticleSettings],
                                 sections: Map[DayOfWeek, Section])

final case class WeatherArticleSettings(title: String,
                                        byline: String,
                                        `abstract`: Option[String],
                                        pageNumber: Int,
                                        image: Option[Image],
                                        cities: Seq[String]) {
  def articleAbstract: Option[String] = `abstract`
}
