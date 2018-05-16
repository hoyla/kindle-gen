package com.gu

import java.nio.file.{Path, Paths}

import scala.concurrent.duration.{Duration, FiniteDuration}

import com.typesafe.config.Config

package object config {
  implicit class RichConfig(val config: Config) extends AnyVal {
    def getPath(key: String): Path = Paths.get(config.getString(key))
    def getFiniteDuration(key: String): FiniteDuration =
      Duration.fromNanos(config.getDuration(key).toNanos)
  }
}
