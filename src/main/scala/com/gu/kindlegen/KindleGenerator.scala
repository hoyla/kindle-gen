package com.gu.kindlegen

import java.nio.file.{Files, Path}
import java.time.LocalDate

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

import com.gu.kindlegen.Querier.PrintSentContentClient

object KindleGenerator {
  def apply(settings: Settings, editionDate: LocalDate): KindleGenerator = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val capiClient = new PrintSentContentClient(settings.contentApi)
    val querier = new Querier(capiClient, editionDate)
    new KindleGenerator(querier, settings.publishing)
  }
}

class KindleGenerator(querier: Querier, publishingSettings: PublishingSettings)(implicit ec: ExecutionContext) {
  def fetchNitfBundle: Seq[File] = {
    val fArticles = querier.fetchAllArticles().flatMap { results =>
      if (results.length >= publishingSettings.minArticlesPerEdition)
        Future.successful(results)
      else
        Future.failed(new RuntimeException(s"${results.length} articles is not enough to generate an edition!"))
    }

    val fMaybeImages = fArticles.flatMap(Future.traverse(_)(querier.downloadArticleImage))

    val files = fArticles.zip(fMaybeImages).map { case (articles, maybeImages) =>
      articles.zip(maybeImages).zipWithIndex.flatMap { case ((article, maybeImage), index) =>
        Some(articleToFile(article, index)) ++
          maybeImage.map(articleImageToFile(_, index))
      }
    }

    Await.result(files, 2.minutes)
  }

  def writeNitfBundleToDisk(): Seq[Path] = {
    val outputDir = publishingSettings.outputDir
    Files.createDirectories(outputDir)

    fetchNitfBundle.map(file => {
      val data = file.data
      val fileName = file.path
      bytesToFile(data, fileName, outputDir)
    })
  }

  def bytesToFile(data: Array[Byte], fileName: String, outputDirectory: Path): Path = {
    val filePath = outputDirectory.resolve(fileName)
    Files.write(filePath, data)
  }

  private def articleToFile(article: Article, index: Int): File = {
    val nitf = ArticleNITF(article)
    File(
      path = s"${index}_${article.fileName}",
      data = nitf.fileContents.getBytes
    )
  }

  private def articleImageToFile(image: ImageData, index: Int): File = {
    File(
      path = s"${index}_${image.metadata.id}.${image.fileExtension}",
      data = image.data
    )
  }
}
