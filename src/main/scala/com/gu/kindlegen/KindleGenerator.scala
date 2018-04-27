package com.gu.kindlegen

import java.nio.file.{Files, Path}
import java.time.LocalDate

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

import com.gu.io.IOUtils._
import com.gu.kindlegen.Link.PathLink
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
  private val outputDirectory = publishingSettings.outputDir

  def fetchNitfBundle(): Future[Seq[Article]] = {
    val fArticles = querier.fetchAllArticles().flatMap { results =>
      if (results.length >= publishingSettings.minArticlesPerEdition)
        Future.successful(results)
      else
        Future.failed(new RuntimeException(s"${results.length} articles is not enough to generate an edition!"))
    }

    fArticles
  }

  def writeNitfBundleToDisk(): Seq[Path] = {
    Files.createDirectories(outputDirectory)

    val fArticlesOnDisk = fetchNitfBundle().flatMap { articles =>
      Future.sequence(articles.zipWithIndex.map { case (article, index) =>
        downloadMainImage(article, index)
          .map(writeToFile(_, index))
      })
    }

    Await.result(fArticlesOnDisk, 5.minutes)
      .flatMap { article =>
        Some(article.link) ++ article.mainImage.map(_.link)
      }.collect {
        case link: PathLink => link.toPath
      }
    // TODO write manifests
  }

  private def downloadMainImage(article: Article, fileNameIndex: Int): Future[Article] = {
    if (publishingSettings.downloadImages) {
      querier.downloadArticleImage(article).map { maybeImageData =>
        article.copy(mainImage =
          maybeImageData.map(writeToFile(_, fileNameIndex))
        )
      }
    } else {
      Future.successful(article)  // with image sources as URLs
    }
  }

  private def writeToFile(image: ImageData, fileNameIndex: Int): Image = {
    val fileName = s"${fileNameIndex}_${image.metadata.id}.${image.fileExtension}"
    val path = writeToFile(image.data, fileName)
    image.metadata.copy(link = Link.AbsolutePath.from(path.toRealPath()))
  }

  private def writeToFile(article: Article, fileNameIndex: Int): Article = {
    val nitf = ArticleNITF(article)
    val fileName = s"${fileNameIndex}_${asFileName(article.docId)}.nitf"
    val path = writeToFile(nitf.fileContents.getBytes("UTF-8"), fileName)
    article.copy(link = Link.AbsolutePath.from(path.toRealPath()))
  }

  private def writeToFile(data: Array[Byte], fileName: String): Path = {
    val filePath = outputDirectory.resolve(fileName)
    Files.write(filePath, data)
  }
}
