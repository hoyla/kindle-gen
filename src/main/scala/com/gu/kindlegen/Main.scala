package com.gu.kindlegen

import scala.util.{ Failure, Success, Try }

object Main extends App {
  Settings.load match {
    case Success(settings) => run(settings)
    case Failure(error) => handleConfigError(error)
  }

  private def run(settings: Settings): Unit = {
    val kindleGenerator = new KindleGenerator(settings)
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
