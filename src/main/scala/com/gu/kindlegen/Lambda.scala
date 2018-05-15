package com.gu.kindlegen

import java.nio.file.Files
import java.time.{Instant, LocalDate}
import java.time.ZoneOffset.UTC

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.typesafe.config.{Config, ConfigFactory}

import com.gu.io.Link.RelativePath
import com.gu.io.aws.{S3Publisher, S3Settings}

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
    val bucketName = config.getString("serialization.s3.bucket")
    val bucketDirectory = config.getString("serialization.s3.prefix")

    val s3 = AmazonS3ClientBuilder.defaultClient()
    // TODO should we validate the connection and permissions?

    Settings(config) match {
      case Success(settings) =>
        import scala.concurrent.ExecutionContext.Implicits.global

        val fileSettings = settings.publishing.files
        val originalOutputDir = fileSettings.outputDir.toAbsolutePath
        val customFileSettings = fileSettings.copy(outputDir = originalOutputDir.resolve(date.toString))

        val publisher = S3Publisher(s3, S3Settings(bucketName, s"$bucketDirectory/$date", fileSettings.outputDir))
        val kindleGenerator = KindleGenerator(settings.withPublishingFiles(customFileSettings), date, publisher)

        logger.log(s"Generating for $date; writing to ${customFileSettings.outputDir}")
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
}
