package com.gu.kindlegen

import java.nio.file.{Files, Paths}
import java.time.{Instant, LocalDate, ZoneOffset}

import com.amazonaws.services.lambda.runtime.Context
import scala.collection.JavaConverters._
import scala.util.{Failure, Success}

object Lambda {
  // env.AWS_REGION -> eu-west-1
  // env.Stage -> CODE

  /*
   * This is your lambda entry point
   */
  def handler(parameters: java.util.Map[String, String], context: Context): Unit = {
    val logger = context.getLogger

    val params = parameters.asScala
    val date =
      params.get("date").map(LocalDate.parse)  // custom parameter in the test view
        .orElse(params.get("time").map(Instant.parse(_).atZone(ZoneOffset.UTC).toLocalDate))  // scheduled event
        .getOrElse(LocalDate.now)

    val tmpFiles = Files.list(Paths.get("/tmp")).iterator.asScala.toList
    println(s"Found ${tmpFiles.length} tmp files:\n${tmpFiles.mkString(", ")}")

    val newFile = Files.createFile(Paths.get(s"/tmp/$date"))
    println(s"Created a new temp file: $newFile")

//    logger.log(s"Generating for $date")
//
//    Settings.load match {
//      case Success(settings) =>
//        val kindleGenerator = KindleGenerator(settings, date)
//        kindleGenerator.writeNitfBundleToDisk()
//        // TODO write the results to S3
//      case Failure(error) => logger.log(s"[ERROR] Could not load the configuration! $error")
//    }
  }

}
