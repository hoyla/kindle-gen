package com.gu.kindlegen

import java.nio.file.{Files, Paths}
import java.time.LocalDate

import scala.util.{Failure, Success}

object Main extends App {
  Settings.load match {
    case Success(settings) => run(settings)
    case Failure(error) => handleConfigError(error)
  }

  private def run(settings: Settings): Unit = {
    val kindleGenerator = new KindleGenerator(settings.contentApi, LocalDate.now)
    kindleGenerator.writeNitfBundleToDisk(Files.createDirectories(Paths.get("tmp")))
    println("Done!")
    // Why does the program not exit here?
  }

  private def handleConfigError(error: Throwable) = {
    System.err.println("Couldn't load the configuration!")
    error.printStackTrace()
    System.exit(1)
  }
}
