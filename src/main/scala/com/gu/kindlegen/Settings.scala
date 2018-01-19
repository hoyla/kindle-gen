package com.gu.kindlegen

import com.typesafe.config.{ Config, ConfigFactory }

import scala.util.Try

/** Encapsulates the settings of this application */
final case class Settings(contentApiKey: String, contentApiTargetUrl: String)

object Settings {
  def load: Try[Settings] = {
    apply(ConfigFactory.load)
  }

  def apply(config: Config): Try[Settings] = {
    import ConfigKeys._
    for {
      contentApiKey <- Try(config.getString(ContentApiKey))
      contentApiUrl <- Try(config.getString(ContentApiTargetUrl))
    } yield {
      Settings(contentApiKey, contentApiUrl)
    }
  }

  private object ConfigKeys {
    val ContentApiKey = "content-api.key"
    val ContentApiTargetUrl = "content-api.url"
  }
}
