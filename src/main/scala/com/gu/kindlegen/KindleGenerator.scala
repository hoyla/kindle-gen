package com.gu.kindlegen

import java.nio.file.{ Files, Paths }
import java.io.{ BufferedOutputStream, ByteArrayInputStream, FileOutputStream, InputStream }

import scala.concurrent.Await
import scala.concurrent.duration._

class KindleGenerator {
}
object KindleGenerator {
  val Querier = new Querier

  def getNitfBundle: Seq[File] = {
    // TODO: Wrap all these up in one method on the Querier so when
    // we use it we don't have to call all three.
    // More concise, and removes some chance of forgetting one.
    val searchResponses = Querier.getAllPagesContent
    val contents = Querier.responsesToContent(searchResponses)
    val sortedContent = Querier.sortContentByPageAndSection(contents)

    // TODO: Move these methods into their own classes
    val articles = Querier.responseToArticles(sortedContent)
    val images = Await.result(
      Querier.getAllArticleImages(articles),
      120.seconds
    )

    val files = articles.map(articleToFile) ++ images.map(articleImageToFile)
    files
  }

  def getNitfBundleToDisk: Unit = {
    getNitfBundle.foreach(file => {
      val data = file.data
      val fileName = file.path
      bytesToFile(data, fileName)
    })
  }

  def bytesToFile(data: Array[Byte], fileName: String): Unit = {
    val fileFolder = "tmp"
    val filePath = s"${fileFolder}/${fileName}"
    Files.write(Paths.get(filePath), data)
  }

  private def articleToFile(article: Article): File = {
    val nitf = ArticleNITF(article)
    File(
      path = article.fileName,
      data = nitf.fileContents.getBytes
    )
  }

  private def articleImageToFile(image: ArticleImage): File = {
    File(
      path = s"${image.articleId}.jpg",
      data = image.data
    )
  }
}
