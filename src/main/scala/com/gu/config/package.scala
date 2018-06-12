package com.gu

import java.nio.file.{Path, Paths}

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

  implicit val pathReader = new ValueReader[Path] {
    override def read(config: Config, path: String): Path =
      Paths.get(config.as[String](path))
  }
}
