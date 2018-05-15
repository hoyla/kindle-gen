package com.gu.kindlegen

import java.nio.file.Files
import java.time.{Instant, LocalDate}
import java.time.ZoneOffset.UTC

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.typesafe.config.{Config, ConfigFactory}

import com.gu.kindlegen.Link.RelativePath

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
    val pathPrefix = config.getString("serialization.s3.prefix")

    val s3 = AmazonS3ClientBuilder.defaultClient()
    // TODO should we validate the connection and permissions?

    Settings(config) match {
      case Success(settings) =>
        import scala.concurrent.ExecutionContext.Implicits.global

        val fileSettings = settings.publishing.files
        val originalOutputDir = fileSettings.outputDir.toAbsolutePath
        val customFileSettings = fileSettings.copy(outputDir = originalOutputDir.resolve(date.toString))
        val kindleGenerator = KindleGenerator(settings.withPublishingFiles(customFileSettings), date)

        logger.log(s"Generating for $date; writing to ${customFileSettings.outputDir}")
        val files = kindleGenerator.writeNitfBundleToDisk()

        // FIXME Future#foreach swallows errors; we need to report them
        files.foreach { links => links.collect { case x: RelativePath => x.toPath }.foreach { path =>
          // TODO what if the file already exists in S3? What if the directory already exists and is full?
          val s3path = pathPrefix + originalOutputDir.relativize(path.toAbsolutePath)
          logger.log(s"Uploading $s3path")
          s3.putObject(bucketName, s3path, path.toFile)
          // TODO what if uploading to S3 fails?
          Try(Files.delete(path))  // ignore deletion errors; at worst, the file will consume some space affecting the next invocation
        } }

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
