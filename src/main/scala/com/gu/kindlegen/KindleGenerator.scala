package com.gu.kindlegen

import scala.concurrent.Await
import scala.concurrent.duration._

class KindleGenerator {
}
object KindleGenerator {
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
      // Write file to disk here using the File's path and data...
    })
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
