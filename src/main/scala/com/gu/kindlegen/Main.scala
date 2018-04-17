package com.gu.kindlegen

import scala.util.{ Failure, Success, Try }
import java.time.Instant

object Main extends App {
  Settings.load match {
    case Success(settings) => run(settings)
    case Failure(error) => handleConfigError(error)
  }

  private def run(settings: Settings): Unit = {
    val kindleGenerator = new KindleGenerator(settings, Instant.now)
    kindleGenerator.getNitfBundleToDisk
    println("Done!")
    // Why does the program not exit here?
  }

  private def handleConfigError(error: Throwable) = {
    System.err.println("Couldn't load the configuration!")
    error.printStackTrace()
    System.exit(1)
  }
}
