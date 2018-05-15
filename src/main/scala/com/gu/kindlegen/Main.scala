package com.gu.kindlegen

import java.time.LocalDate

import scala.util.{Failure, Success}

import com.gu.io.FilePublisher

object Main extends App {
  Settings.load match {
    case Success(settings) => run(settings)
    case Failure(error) => handleConfigError(error)
  }

  private def run(settings: Settings): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val publisher = FilePublisher(settings.publishing.files.outputDir)
    val kindleGenerator = KindleGenerator(settings, LocalDate.now, publisher)
    kindleGenerator.publish()
    println("Done!")
    // Why does the program not exit here?
  }

  private def handleConfigError(error: Throwable) = {
    System.err.println("Couldn't load the configuration!")
    error.printStackTrace()
    System.exit(1)
  }
}
