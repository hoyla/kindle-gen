package com.gu.kindlegen

import java.nio.file.Files
import java.time.{Instant, LocalDate}
import java.time.ZoneOffset.UTC

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

import awscala._
import awscala.s3._
import com.amazonaws.services.lambda.runtime.Context

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

    val stage = sys.env("Stage")  // CODE or PROD
    val region = Region(sys.env("AWS_REGION"))
    val bucketName = "kindle-gen-published-files"

    implicit val s3: S3 = S3.at(region)
    val bucket = s3.bucket(bucketName).getOrElse(throw new RuntimeException("Cannot find bucket"))

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
          val s3path = stage + "/" + originalOutputDir.relativize(path.toAbsolutePath)
          logger.log(s"Uploading $s3path")
          bucket.put(s3path, path.toFile)
          Try(Files.delete(path))  // ignore deletion errors; at worst, the file will consume some space affecting the next invocation
        }

      case Failure(error) =>
        logger.log(s"[ERROR] Could not load the configuration! $error")
        throw error
    }
  }

}
