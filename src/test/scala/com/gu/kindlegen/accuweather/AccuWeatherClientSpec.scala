package com.gu.kindlegen.accuweather

import java.net.URI

import scala.util.{Failure, Success}

import org.json4s._
import org.json4s.native.JsonMethods._
import org.scalatest.{Assertion, FunSpec}
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.Inspectors._
import org.scalatest.time.SpanSugar._
import org.scalatest.OptionValues._

import com.gu.concurrent.TestExecutionContext._
import com.gu.io.sttp.{OkHttpSttpDownloader, SttpDownloaderStub}
import com.gu.kindlegen.app.Settings
import com.gu.kindlegen.weather.WeatherClient._

class AccuWeatherClientSpec extends FunSpec {
  import AccuWeatherClient._

  describe("singleForecastReader") {
    it("parses AccuWeather's 1-day forecast response") {
      val json = parse(forecastResponse)
      val actual = singleForecastReader.read(json)
      actual shouldBe expectedForecast
    }
  }

  describe("knownLocations") {
    it("is not empty") {
      knownLocations should not be empty
      knownLocations.size should be >= 3
    }

    it("contains valid locations") {
      forEvery(knownLocations) { location: Location =>
        location.name should not be empty
        location.key should not be empty
      }
    }

    it("contains unique locations") {
      def checkUniqueness(property: Location => String) =
        forEvery(knownLocations.map(property)) { value => knownLocations.count(property(_) == value) shouldBe 1 }
      checkUniqueness(_.name)
      checkUniqueness(_.key)
    }

    it("contains all the locations in the default config") {
      withSettings { settings =>
        val knownCities = knownLocations.map(_.name)
        val defaultCities = settings.weather.articles.flatMap(_.cities)
        knownCities should contain allElementsOf defaultCities
      }
    }
  }

  describe("forecastFor(location)") {
    it("fetches a forecast for a particular location") {
      val downloader = SttpDownloaderStub {
        _.whenRequestMatches(_.uri.path.lastOption.exists(_.matches(raw"${cairo.key}(?:\.json)?")))
          .thenRespond(forecastResponse)
      }

      val client = AccuWeatherClient(apiKey = "", new URI("http://example.com"), downloader)
      val actual = client.forecastFor(cairo).futureValue
      actual shouldBe expectedForecast
    }

    it("works with the actual API") {
      withSettings { settings =>
        val credentials = settings.accuWeather
        val downloader = OkHttpSttpDownloader()
        val client = AccuWeatherClient(credentials.apiKey, credentials.baseUrl, downloader)

        val forecast = client.forecastFor(cairo).futureValue(timeout(scaled(15.seconds)))
        withClue(forecast) {
          forecast.nonEmpty shouldBe true
          forecast.high.value should be < 55  // the temperatures should be in celsius, not fahrenheit
          forecast.low.value should be < 40  // these numbers are higher than the highest recorded temperatures in Cairo
                                            //  but still very low for Cairo if they were in fahrenheit
        }
      }
    }
  }

  private def withSettings(f: Settings => Assertion): Unit = {
    Settings.load match {
      case Success(settings) => f(settings)
      case Failure(_) => pending // fix SettingsSpec first
    }
  }

  private val cairo = Location("Cairo", "127164")
  private val expectedForecast = Forecast(Some("heat"), Some(34), Some(22), Some("Good"), Some(0))
  private val forecastResponse = """{
    "Headline": {
      "EffectiveDate": "2018-06-07T07:00:00+02:00",
      "EffectiveEpochDate": 1528347600,
      "Severity": 7,
      "Text": "Very warm Thursday",
      "Category": "heat",
      "EndDate": "2018-06-07T19:00:00+02:00",
      "EndEpochDate": 1528390800,
      "MobileLink": "http://m.accuweather.com/en/eg/cairo/127164/extended-weather-forecast/127164?unit=c&lang=en-gb",
      "Link": "http://www.accuweather.com/en/eg/cairo/127164/daily-weather-forecast/127164?unit=c&lang=en-gb"
    },
    "DailyForecasts": [
      {
        "Date": "2018-06-06T07:00:00+02:00",
        "EpochDate": 1528261200,
        "Sun": {
          "Rise": "2018-06-06T04:53:00+02:00",
          "EpochRise": 1528253580,
          "Set": "2018-06-06T18:54:00+02:00",
          "EpochSet": 1528304040
        },
        "Moon": {
          "Rise": null,
          "EpochRise": null,
          "Set": "2018-06-06T11:28:00+02:00",
          "EpochSet": 1528277280,
          "Phase": "Last",
          "Age": 22
        },
        "Temperature": {
          "Minimum": {
            "Value": 22,
            "Unit": "C",
            "UnitType": 17
          },
          "Maximum": {
            "Value": 33.9,
            "Unit": "C",
            "UnitType": 17
          }
        },
        "RealFeelTemperature": {
          "Minimum": {
            "Value": 21.3,
            "Unit": "C",
            "UnitType": 17
          },
          "Maximum": {
            "Value": 37.6,
            "Unit": "C",
            "UnitType": 17
          }
        },
        "RealFeelTemperatureShade": {
          "Minimum": {
            "Value": 21.3,
            "Unit": "C",
            "UnitType": 17
          },
          "Maximum": {
            "Value": 32.6,
            "Unit": "C",
            "UnitType": 17
          }
        },
        "HoursOfSun": 14,
        "DegreeDaySummary": {
          "Heating": {
            "Value": 0,
            "Unit": "C",
            "UnitType": 17
          },
          "Cooling": {
            "Value": 10,
            "Unit": "C",
            "UnitType": 17
          }
        },
        "AirAndPollen": [
          {
            "Name": "AirQuality",
            "Value": 0,
            "Category": "Good",
            "CategoryValue": 1,
            "Type": "Ozone"
          },
          {
            "Name": "Grass",
            "Value": 0,
            "Category": "Low",
            "CategoryValue": 1
          },
          {
            "Name": "Mold",
            "Value": 0,
            "Category": "Low",
            "CategoryValue": 1
          },
          {
            "Name": "Ragweed",
            "Value": 0,
            "Category": "Low",
            "CategoryValue": 1
          },
          {
            "Name": "Tree",
            "Value": 0,
            "Category": "Low",
            "CategoryValue": 1
          },
          {
            "Name": "UVIndex",
            "Value": 12,
            "Category": "Extreme",
            "CategoryValue": 5
          }
        ],
        "Day": {
          "Icon": 1,
          "IconPhrase": "Sunny",
          "ShortPhrase": "Plenty of sunshine; pleasant",
          "LongPhrase": "Plenty of sunshine; pleasant",
          "PrecipitationProbability": 0,
          "ThunderstormProbability": 0,
          "RainProbability": 0,
          "SnowProbability": 0,
          "IceProbability": 0,
          "Wind": {
            "Speed": {
              "Value": 11.1,
              "Unit": "km/h",
              "UnitType": 7
            },
            "Direction": {
              "Degrees": 4,
              "Localized": "N",
              "English": "N"
            }
          },
          "WindGust": {
            "Speed": {
              "Value": 24.1,
              "Unit": "km/h",
              "UnitType": 7
            },
            "Direction": {
              "Degrees": 356,
              "Localized": "N",
              "English": "N"
            }
          },
          "TotalLiquid": {
            "Value": 0,
            "Unit": "mm",
            "UnitType": 3
          },
          "Rain": {
            "Value": 0,
            "Unit": "mm",
            "UnitType": 3
          },
          "Snow": {
            "Value": 0,
            "Unit": "cm",
            "UnitType": 4
          },
          "Ice": {
            "Value": 0,
            "Unit": "mm",
            "UnitType": 3
          },
          "HoursOfPrecipitation": 0,
          "HoursOfRain": 0,
          "HoursOfSnow": 0,
          "HoursOfIce": 0,
          "CloudCover": 0
        },
        "Night": {
          "Icon": 33,
          "IconPhrase": "Clear",
          "ShortPhrase": "Clear",
          "LongPhrase": "Clear",
          "PrecipitationProbability": 0,
          "ThunderstormProbability": 0,
          "RainProbability": 0,
          "SnowProbability": 0,
          "IceProbability": 0,
          "Wind": {
            "Speed": {
              "Value": 14.8,
              "Unit": "km/h",
              "UnitType": 7
            },
            "Direction": {
              "Degrees": 31,
              "Localized": "NNE",
              "English": "NNE"
            }
          },
          "WindGust": {
            "Speed": {
              "Value": 24.1,
              "Unit": "km/h",
              "UnitType": 7
            },
            "Direction": {
              "Degrees": 25,
              "Localized": "NNE",
              "English": "NNE"
            }
          },
          "TotalLiquid": {
            "Value": 0,
            "Unit": "mm",
            "UnitType": 3
          },
          "Rain": {
            "Value": 0,
            "Unit": "mm",
            "UnitType": 3
          },
          "Snow": {
            "Value": 0,
            "Unit": "cm",
            "UnitType": 4
          },
          "Ice": {
            "Value": 0,
            "Unit": "mm",
            "UnitType": 3
          },
          "HoursOfPrecipitation": 0,
          "HoursOfRain": 0,
          "HoursOfSnow": 0,
          "HoursOfIce": 0,
          "CloudCover": 0
        },
        "Sources": [
          "AccuWeather"
        ],
        "MobileLink": "http://m.accuweather.com/en/eg/cairo/127164/daily-weather-forecast/127164?day=1&unit=c&lang=en-gb",
        "Link": "http://www.accuweather.com/en/eg/cairo/127164/daily-weather-forecast/127164?day=1&unit=c&lang=en-gb"
      }
    ]
  }"""
}
