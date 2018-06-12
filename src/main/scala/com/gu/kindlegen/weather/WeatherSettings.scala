package com.gu.kindlegen.weather

import com.gu.kindlegen.{Image, Section}


final case class WeatherSettings(section: Section,
                                 minForecastsPercentage: Int,  // how many successful retrievals of forecasts to create an article
                                 articles: Seq[WeatherArticleSettings])

final case class WeatherArticleSettings(title: String,
                                        byline: String,
                                        `abstract`: Option[String],
                                        image: Option[Image],
                                        cities: Seq[String]) {
  def articleAbstract: Option[String] = `abstract`
}
