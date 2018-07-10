package com.gu.kindlegen.app

import java.nio.file.{Files, Path}
import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit.HOURS

import scala.collection.JavaConverters._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration, _}
import scala.util.Failure

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.config.ConfigException.BadValue
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.scala.Logging

import com.gu.io.aws.{S3Publisher, S3PublisherSettings}
import com.gu.io.sttp.{OkHttpSttpDownloader, SttpDownloader}
import com.gu.kindlegen._
import com.gu.kindlegen.accuweather.AccuWeatherClient
import com.gu.kindlegen.capi.GuardianArticlesProvider
import com.gu.kindlegen.weather.DailyWeatherForecastProvider


/** @param localHours Run within 60 minutes of any of these hours
  * @param zone the zone used to interpret ''localHours'' and to format the date for ''outputDirDateFormat''
  * @param outputDirDateFormat a date/time format used to generate the output directory name
  */
final case class RunSettings(localHours: Seq[LocalTime],
                             zone: ZoneId,
                             outputDirDateFormat: String)


final case class S3Settings(bucketName: String,
                            bucketDirectory: String,
                            publicDirectory: String,
                            optionalTmpDirOnDisk: Option[Path])
    extends S3PublisherSettings {
  lazy val tmpDirOnDisk: Path = optionalTmpDirOnDisk.getOrElse(Files.createTempDirectory(""))
}


object Lambda extends Logging {
  private val ConfigSettingsEnvKey = "ConfigSettings"
  private val LogLevelConfigKey = "logLevel"
  private val DefaultConfig = s"$LogLevelConfigKey = ALL" // used in case ConfigSettings isn't available (probably an error)

  private val ErrorReportingTimeInMillis = 10

  private case class Params(date: LocalDate, forceRun: Boolean)

  def handler(parameters: java.util.Map[String, Any], context: Context): Unit = {
    val config = resolveConfig()
    configureLogging(config)

    Settings(config)
      .recoverWith(fatalError("Could not parse the configuration settings!"))
      .map { settings =>
        val params = resolveParams(parameters, settings)
        val adaptedSettings = adaptSettings(settings, params)

        val lambda = new Lambda(adaptedSettings, params.date)
        lambda.run(params.forceRun, context.getRemainingTimeInMillis)
      }
      .recoverWith(fatalError("Generation failed!"))
      .get  // throw any errors to indicate failure
  }

  private def resolveConfig(): Config = {
    val config = ConfigFactory.load
    val envConfig = sys.env.getOrElse(ConfigSettingsEnvKey, DefaultConfig)
    val overrides = ConfigFactory.parseString(envConfig)
    overrides.withFallback(config).resolve()
  }

  // configure logging _before_ parsing the config into Settings
  // this helps with logging errors during startup and while parsing settings
  private def configureLogging(config: Config): Unit = {
    if (config.hasPath(LogLevelConfigKey)) {
      val levelName = config.getString(LogLevelConfigKey)
      val level = Option(Level.getLevel(levelName))
        .getOrElse(throw new BadValue(config.origin, LogLevelConfigKey, s"""Log level "$levelName" is unknown!"""))
      Configurator.setRootLevel(level)
      Configurator.setAllLevels("com.gu", level)
    }
  }

  private def resolveParams(parameters: java.util.Map[String, Any], settings: Settings): Params = {
    logger.trace(s"Running with parameters $parameters")
    val params = parameters.asScala.mapValues(_.toString)

    val forceRun = params.getOrElse("forceRun", "false").toBoolean

    val zone = settings.run.zone
    val date =
      params.get("date").map(LocalDate.parse)
        .orElse(params.get("time").map(Instant.parse(_).atZone(zone).toLocalDate)) // scheduled event
        .getOrElse(LocalDate.now(zone))

    Params(date, forceRun)
  }

  private def adaptSettings(settings: Settings, params: Params): Settings = {
    withOutputDirForDate(params.date, settings)
  }

  private def withOutputDirForDate(date: LocalDate, settings: Settings): Settings = {
    val dateTime = date.atTime(LocalTime.now(settings.run.zone))
    val formatter = DateTimeFormatter.ofPattern(settings.run.outputDirDateFormat)
    val formattedDate = formatter.format(dateTime)

    if (formattedDate.isEmpty) {
      settings
    } else {
      val fileSettings = settings.publishing.files
      val originalOutputDir = fileSettings.outputDir.toAbsolutePath
      val customFileSettings = fileSettings.copy(outputDir = originalOutputDir.resolve(formattedDate))

      val s3Settings = settings.s3
      val bucketDir = s3Settings.bucketDirectory
      val customS3Settings = s3Settings.copy(bucketDirectory = s"$bucketDir/$formattedDate")

      settings.withPublishingFiles(customFileSettings).copy(s3 = customS3Settings)
    }
  }

  private def fatalError(msg: String): PartialFunction[Throwable, Failure[Settings]] = { case error =>
    logger.fatal(msg, error)
    Failure(error)
  }
}

class Lambda(settings: Settings, date: LocalDate) extends Logging {
  import Lambda.ErrorReportingTimeInMillis

  def run(forceRun: Boolean, remainingTimeInMillis: => Long): Unit = {
    if (forceRun || isTimeToRun) {
      logger.debug(s"Running with settings $settings")
      doRun(remainingTimeInMillis)
    } else {
      logger.info(s"Skipping run; it's either too early or too late for " +
        s"${settings.run.localHours.mkString("[", ", ", "]")} ${settings.run.zone}.")
    }
  }

  def doRun(remainingTimeInMillis: => Long): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val publisher = s3Publisher(settings)
    logger.info(s"Starting to publish files for $date; uploading to s3://${settings.s3.absolutePath.source}")

    val deletePublicDir = publisher
      .undirect(settings.s3.publicDirectory)
      .andThen { case Failure(error) => logger.error("Deleting the public directory failed!", error) }

    val generateFiles = deletePublicDir
      .flatMap(_ => doRun(publisher))
      .andThen { case Failure(error) =>
        logger.error("Generating and publishing failed!", error)
      }

    val publishPublicly = generateFiles
      .flatMap(_ => publisher.redirect(settings.s3.publicDirectory))
      .andThen { case Failure(error) => logger.error("Making the directory public failed!", error) }

    Await.result(publishPublicly, Duration(remainingTimeInMillis - ErrorReportingTimeInMillis, MILLISECONDS))
    logger.debug("Publishing finished successfully.")
  }

  private def doRun(publisher: S3Publisher)(implicit ec: ExecutionContext): Future[Unit] = {
    val downloader = OkHttpSttpDownloader()

    val provider = new CompositeArticlesProvider(
      capiProvider(settings, downloader),
      weatherProvider(settings, downloader)
    )

    val binder = MainSectionsBookBinder(settings.books.mainSections)

    val kindleGenerator =
      new KindleGenerator(provider, binder, publisher, downloader, settings.articles.downloadTimeout, settings.publishing)

    kindleGenerator.publish()
  }

  private def isTimeToRun: Boolean = {
    val runSettings = settings.run
    val localNow = LocalTime.now(runSettings.zone)

    runSettings.localHours.exists { localHour =>
      localNow.isAfter(localHour) &&
        HOURS.between(localHour, localNow) == 0
    }
  }

  private def capiProvider(settings: Settings, downloader: OkHttpSttpDownloader)
                          (implicit ec: ExecutionContext): ArticlesProvider = {
    GuardianArticlesProvider(settings.contentApi, settings.articles, downloader, date)
  }

  private def weatherProvider(settings: Settings, downloader: SttpDownloader)
                             (implicit ec: ExecutionContext): ArticlesProvider = {
    val client = AccuWeatherClient(settings.accuWeather, downloader)
    val section = settings.weather.sections(date.getDayOfWeek)
    val editionDate = date.atStartOfDay.atOffset(ZoneOffset.UTC)  // same offset as CAPI articles
    new DailyWeatherForecastProvider(client, section, editionDate, settings.weather)
  }

  private def s3Publisher(settings: Settings)(implicit ec: ExecutionContext): S3Publisher = {
    val bucketName = settings.s3.bucketName
    require(bucketName.nonEmpty, "S3 bucket name was not specified")

    val s3 = AmazonS3ClientBuilder.defaultClient()
    require(s3.doesBucketExistV2(bucketName), s"S3 bucket $bucketName was not found")

    S3Publisher(s3, settings.s3)
  }
}
