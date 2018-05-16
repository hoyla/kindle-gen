package com.gu.kindlegen

import java.time.{Instant, LocalDate}
import java.time.ZoneOffset.UTC

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.typesafe.config.{Config, ConfigFactory}

import com.gu.io.aws.S3Publisher

object Lambda {
  /*
   * This is your lambda entry point
   */
  def handler(parameters: java.util.Map[String, Any], context: Context): Unit = {
    val logger = context.getLogger

    val params = parameters.asScala.mapValues(_.toString)

    val date =
      params.get("date").map(LocalDate.parse)
        .orElse(params.get("time").map(Instant.parse(_).atZone(UTC).toLocalDate))  // scheduled event
        .getOrElse(LocalDate.now)

    val config = resolveConfig(sys.env.getOrElse("ConfigSettings", ""))

    Settings(config).map(withOutputDirForDate(date, _)) match {
      case Success(settings) =>
        import scala.concurrent.ExecutionContext.Implicits.global

        val publisher = s3Publisher(settings.s3)
        val kindleGenerator = KindleGenerator(settings, date, publisher)

        logger.log(s"Generating for $date; uploading to s3://${settings.s3.absolutePath.source}")
        kindleGenerator.publish()

      case Failure(error) =>
        logger.log(s"[ERROR] Could not load the configuration! $error")
        throw error
    }
  }

  private def resolveConfig(configString: String): Config = {
    val config = ConfigFactory.load
    val overrides = ConfigFactory.parseString(configString)
    overrides.withFallback(config).resolve()
  }

  private def withOutputDirForDate(date: LocalDate, settings: Settings) = {
    val fileSettings = settings.publishing.files
    val originalOutputDir = fileSettings.outputDir.toAbsolutePath
    val customFileSettings = fileSettings.copy(outputDir = originalOutputDir.resolve(date.toString))
    settings.withPublishingFiles(customFileSettings)
  }

  private def s3Publisher(settings: S3Settings)(implicit ec: ExecutionContext): S3Publisher = {
    val s3 = AmazonS3ClientBuilder.defaultClient()

    val bucketName = settings.bucketName
    require(s3.doesBucketExistV2(bucketName), s"S3 bucket $bucketName was not found")

    S3Publisher(s3, settings)
  }
}
