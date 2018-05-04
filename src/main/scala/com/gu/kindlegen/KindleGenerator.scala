package com.gu.kindlegen

import java.nio.file.{Files, Path}
import java.time.LocalDate

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.xml.Elem

import com.gu.io.IOUtils._
import com.gu.kindlegen.Link.PathLink
import com.gu.kindlegen.Querier.PrintSentContentClient
import com.gu.xml._

object KindleGenerator {
  def apply(settings: Settings, editionDate: LocalDate)(implicit ec: ExecutionContext): KindleGenerator = {
    val capiClient = new PrintSentContentClient(settings.contentApi)
    val querier = new Querier(capiClient, settings.query, editionDate)
    new KindleGenerator(querier, settings.publishing, settings.query)
  }
}

class KindleGenerator(querier: Querier,
                      publishingSettings: PublishingSettings,
                      querySettings: QuerySettings)(implicit ec: ExecutionContext) {
  private def fileSettings = publishingSettings.files
  private lazy val outputDirectory: Path = Files.createDirectories(fileSettings.outputDir).toRealPath()
  private lazy val outputDirLink: Link.AbsolutePath = Link.AbsolutePath.from(outputDirectory)

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
    val fArticlesOnDisk = fetchNitfBundle().flatMap { articles =>
      Future.sequence(articles.zipWithIndex.map { case (article, index) =>
        downloadMainImage(article, index)
          .map(writeToFile(_, index))
      })
    }

    val articlesOnDisk = Await.result(fArticlesOnDisk, querySettings.downloadTimeout)

    val sections = BookSection.fromArticles(articlesOnDisk).map(writeToFile)
    val rootManifest = writeToFile(sections)

    (articlesOnDisk ++ articlesOnDisk.flatMap(_.mainImage) ++ sections :+ rootManifest)
      .map(_.link).collect {
      case link: PathLink => link.toPath
    }
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
    val fileName = s"${fileNameIndex}_${image.metadata.id}.${fileExtension(image.source)}"
    image.metadata.copy(link = writeToFile(image.data, fileName))
  }

  private def writeToFile(article: Article, fileNameIndex: Int): Article = {
    val nitfGenerator = ArticleNITF(article)
    val fileName = s"${fileNameIndex}_${asFileName(article.docId)}.${fileSettings.nitfExtension}"
    article.copy(link = writeToFile(nitfGenerator.nitf, fileName))
  }

  private def writeToFile(bookSection: BookSection): BookSection = {
    val fileName = asFileName(bookSection.id) + "." + fileSettings.rssExtension
    val manifest = ArticlesManifest(bookSection)
    bookSection.withLink(writeToFile(manifest.rss, fileName))
  }

  private def writeToFile(sections: Seq[BookSection]): RssManifest = {
    val fileName = fileSettings.rootManifestFileName
    val manifest = SectionsManifest(
      title = publishingSettings.publicationName,
      link = Link.AbsoluteURL.from(publishingSettings.publicationLink),
      books = sections
    )
    manifest.copy(link = writeToFile(manifest.rss, fileName))
  }

  private def writeToFile(content: Elem, fileName: String): Link.RelativePath = {
    val prettifier = if (publishingSettings.prettifyXml) defaultPrettyPrinter else TrimmingPrinter
    writeToFile(content.toXmlBytes(fileSettings.encoding)(prettifier), fileName)
  }

  private def writeToFile(content: String, fileName: String): Link.RelativePath =
    writeToFile(content.trim.getBytes(fileSettings.encoding), fileName)

  private def writeToFile(content: Array[Byte], fileName: String): Link.RelativePath = {
    val path = write(content, fileName)
    val relativePath = outputDirectory.relativize(path)
    Link.RelativePath.from(relativePath, outputDirLink)
  }

  private def write(data: Array[Byte], fileName: String): Path = {
    val filePath = outputDirectory.resolve(fileName)
    Files.write(filePath, data)
  }
}
