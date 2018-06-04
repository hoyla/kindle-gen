package com.gu.kindlegen

import java.time.{Instant, LocalDate}
import java.time.ZoneOffset.UTC

import scala.collection.JavaConverters._
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.config.ConfigException.BadValue
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.scala.Logging

import com.gu.io.sttp.OkHttpSttpDownloader
import com.gu.io.aws.S3Publisher
import com.gu.kindlegen.capi.GuardianArticlesProvider

object Lambda extends Logging {
  private val ConfigSettingsEnvKey = "ConfigSettings"
  private val LogLevelConfigKey = "logLevel"
  private val DefaultConfig = s"$LogLevelConfigKey = ALL" // used in case ConfigSettings isn't available (probably an error)

  private val ErrorReportingTimeInMillis = 10

  def handler(parameters: java.util.Map[String, Any], context: Context): Unit = {
    val config = resolveConfig()
    configureLogging(config)

    logger.trace(s"Running with parameters $parameters")
    val params = parameters.asScala.mapValues(_.toString)
    val date =
      params.get("date").map(LocalDate.parse)
        .orElse(params.get("time").map(Instant.parse(_).atZone(UTC).toLocalDate)) // scheduled event
        .getOrElse(LocalDate.now)

    Settings(config)
      .recover(fatalError("Could not load the configuration"))
      .map(withOutputDirForDate(date))
      .map(new Lambda(_).run(date, context.getRemainingTimeInMillis))
      .recover(fatalError("Generation failed!"))
  }

  private def resolveConfig(): Config = {
    val config = ConfigFactory.load
    val envConfig = sys.env.getOrElse(ConfigSettingsEnvKey, DefaultConfig)
    val overrides = ConfigFactory.parseString(envConfig)
    overrides.withFallback(config).resolve()
  }

  private def configureLogging(config: Config): Unit = {
    if (config.hasPath(LogLevelConfigKey)) {
      val levelName = config.getString(LogLevelConfigKey)
      val level = Option(Level.getLevel(levelName))
        .getOrElse(throw new BadValue(config.origin, LogLevelConfigKey, s"""Log level "$levelName" is unknown!"""))
      Configurator.setRootLevel(level)
      Configurator.setAllLevels("com.gu", level)
    }
  }

  private def withOutputDirForDate(date: LocalDate)(settings: Settings): Settings = {
    val fileSettings = settings.publishing.files
    val originalOutputDir = fileSettings.outputDir.toAbsolutePath
    val customFileSettings = fileSettings.copy(outputDir = originalOutputDir.resolve(date.toString))

    val s3Settings = settings.s3
    val bucketDir = s3Settings.bucketDirectory
    val customS3Settings = s3Settings.copy(bucketDirectory = s"$bucketDir/$date")

    settings.withPublishingFiles(customFileSettings).copy(s3 = customS3Settings)
  }

  private def fatalError(msg: String): PartialFunction[Throwable, Settings] = { case error =>
    logger.fatal(msg, error)
    throw error
  }
}

class Lambda(settings: Settings) extends Logging {
  import Lambda.ErrorReportingTimeInMillis

  def run(date: LocalDate, remainingTimeInMillis: => Long): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    logger.debug(s"Running with settings $settings")

    val downloader = OkHttpSttpDownloader()
    val publisher = s3Publisher(settings.s3)
    val provider = GuardianArticlesProvider(settings, date)
    val kindleGenerator = KindleGenerator(provider, publisher, downloader, settings)

    logger.info(s"Starting to publish files for $date; uploading to s3://${settings.s3.absolutePath.source}")
    val published = kindleGenerator.publish()

    Await.ready(published, Duration(remainingTimeInMillis - ErrorReportingTimeInMillis, MILLISECONDS))
    logger.debug("Publishing finished successfully.")
  }

  private def s3Publisher(settings: S3Settings)(implicit ec: ExecutionContext): S3Publisher = {
    val bucketName = settings.bucketName
    require(bucketName.nonEmpty, "S3 bucket name was not specified")

    val s3 = AmazonS3ClientBuilder.defaultClient()
    require(s3.doesBucketExistV2(bucketName), s"S3 bucket $bucketName was not found")

    S3Publisher(s3, settings)
  }
}
