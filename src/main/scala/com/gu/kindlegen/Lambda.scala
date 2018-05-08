package com.gu.kindlegen

import java.nio.file.Files
import java.time.{Instant, LocalDate}
import java.time.ZoneOffset.UTC

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.s3.AmazonS3ClientBuilder

object Lambda {
  // env.AWS_REGION -> eu-west-1
  // env.Stage -> CODE

  /*
   * This is your lambda entry point
   */
  def handler(parameters: java.util.Map[String, Any], context: Context): Unit = {
    val logger = context.getLogger

    val params = parameters.asScala
    val date =
      params.get("date").map(_.toString).map(LocalDate.parse)
        .orElse(params.get("time").map(_.toString).map(Instant.parse(_).atZone(UTC).toLocalDate))  // scheduled event
        .getOrElse(LocalDate.now)

    val stage = sys.env.getOrElse("Stage", "CODE")  // CODE or PROD
    val bucketName = "kindle-gen-published-files"

    val s3 = AmazonS3ClientBuilder.defaultClient()
    // TODO should we validate the connection and permissions?

    Settings.load match {
      case Success(settings) =>
        import scala.concurrent.ExecutionContext.Implicits.global

        val fileSettings = settings.publishing.files
        val originalOutputDir = fileSettings.outputDir.toAbsolutePath
        val customFileSettings = fileSettings.copy(originalOutputDir.resolve(date.toString))
        val customSettings = settings.withPublishingFiles(customFileSettings)
        val kindleGenerator = KindleGenerator(customSettings, date)

        logger.log(s"Generating for $date; writing to ${customFileSettings.outputDir}")
        val files = kindleGenerator.writeNitfBundleToDisk()

        files.par.foreach { path =>
          // TODO what if the file already exists in S3?
          val s3path = stage + "/" + originalOutputDir.relativize(path.toAbsolutePath)
          logger.log(s"Uploading $s3path")
          s3.putObject(bucketName, s3path, path.toFile)
          // TODO what if uploading to S3 fails?
          Try(Files.delete(path))  // ignore deletion errors; at worst, the file will consume some space affecting the next invocation
        }

      case Failure(error) =>
        logger.log(s"[ERROR] Could not load the configuration! $error")
        throw error
    }
  }

}
