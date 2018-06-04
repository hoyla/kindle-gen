package com.gu.kindlegen

import java.time.LocalDate

import scala.util.{Failure, Success}

import com.gu.io.FilePublisher
import com.gu.io.sttp.OkHttpSttpDownloader
import com.gu.kindlegen.capi.GuardianArticlesProvider


object Main extends App {
  Settings.load match {
    case Success(settings) => run(settings)
    case Failure(error) => handleConfigError(error)
  }

  private def run(settings: Settings): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val downloader = OkHttpSttpDownloader()
    val publisher = FilePublisher(settings.publishing.files.outputDir)
    val provider = GuardianArticlesProvider(settings, downloader, LocalDate.now)
    val kindleGenerator = KindleGenerator(provider, publisher, downloader, settings)

    kindleGenerator.publish()
    // Why does the program not exit here?
  }

  private def handleConfigError(error: Throwable) = {
    System.err.println("Couldn't load the configuration!")
    error.printStackTrace()
    System.exit(1)
  }
}
