package com.gu.kindlegen

import java.nio.file.{Files, Path}
import java.time.LocalDate

import scala.concurrent.Await
import scala.concurrent.duration._

class KindleGenerator(settings: Settings, editionStart: LocalDate) {
  import scala.concurrent.ExecutionContext.Implicits.global
  val Querier = new Querier(settings, editionStart)

  def getNitfBundle: Seq[File] = {
    // TODO: Wrap all these up in one method on the Querier so when
    // we use it we don't have to call all three.
    // More concise, and removes some chance of forgetting one.
    val contents = Querier.fetchAllContents()
    val articles = Querier.responseToArticles(contents)

    articles.zipWithIndex.flatMap { case (article, index) =>
      Some(articleToFile(article, index)) ++
        Querier.getArticleImage(article).map(Await.result(_, 15.seconds)).map(articleImageToFile(_, index))
    }
  }

  def getNitfBundleToDisk(outputDirectory: Path): Unit = {
    getNitfBundle.foreach(file => {
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
