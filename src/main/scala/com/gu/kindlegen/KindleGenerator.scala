package com.gu.kindlegen

import java.nio.file.{Files, Path}
import java.time.LocalDate

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class KindleGenerator(settings: Settings, editionStart: LocalDate) {
  import scala.concurrent.ExecutionContext.Implicits.global
  val Querier = new Querier(settings, editionStart)

  def fetchNitfBundle: Seq[File] = {
    val fArticles = Querier.fetchAllArticles()
    val fMaybeImages = fArticles.flatMap(Future.traverse(_)(Querier.downloadArticleImage))

    val files = fArticles.zip(fMaybeImages).map { case (articles, maybeImages) =>
      articles.zip(maybeImages).zipWithIndex.flatMap { case ((article, maybeImage), index) =>
        Some(articleToFile(article, index)) ++
          maybeImage.map(articleImageToFile(_, index))
      }
    }

    Await.result(files, 2.minutes)
  }

  def writeNitfBundleToDisk(outputDirectory: Path): Unit = {
    fetchNitfBundle.foreach(file => {
      val data = file.data
      val fileName = file.path
      bytesToFile(data, fileName, outputDirectory)
    })
  }

  def bytesToFile(data: Array[Byte], fileName: String, outputDirectory: Path): Unit = {
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
