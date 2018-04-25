package com.gu.kindlegen

import java.time.LocalDate

import scala.util.{Failure, Success}

object Main extends App {
  Settings.load match {
    case Success(settings) => run(settings)
    case Failure(error) => handleConfigError(error)
  }

  private def run(settings: Settings): Unit = {
    val kindleGenerator = KindleGenerator(settings, LocalDate.now)
    kindleGenerator.writeNitfBundleToDisk()
    println("Done!")
    // Why does the program not exit here?
  }

  private def handleConfigError(error: Throwable) = {
    System.err.println("Couldn't load the configuration!")
    error.printStackTrace()
    System.exit(1)
  }
}
