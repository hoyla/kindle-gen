package com.gu

import java.nio.file.{Path, Paths}
import java.time.{LocalTime, ZoneId}

import scala.concurrent.duration.{Duration, FiniteDuration}

import com.typesafe.config.Config
import net.ceedubs.ficus.readers.ValueReader
import net.ceedubs.ficus.Ficus._

package object config {
  implicit class RichConfig(val config: Config) extends AnyVal {
    def getPath(key: String): Path = Paths.get(config.getString(key))

    def getFiniteDuration(key: String): FiniteDuration =
      Duration.fromNanos(config.getDuration(key).toNanos)
  }

  implicit val localTimeReader: ValueReader[LocalTime] =
    (config: Config, path: String) => LocalTime.parse(config.as[String](path))

  implicit val pathReader: ValueReader[Path] =
    (config: Config, path: String) => Paths.get(config.as[String](path))

  implicit val zoneIdReader: ValueReader[ZoneId] =
    (config: Config, path: String) => ZoneId.of(config.as[String](path))

}
