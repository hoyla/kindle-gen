package com.gu.kindlegen

import java.nio.file.{Files, Path}
import java.time.LocalDate

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.xml.Elem

import com.gu.io.IOUtils._
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

  def writeNitfBundleToDisk(): Future[Seq[Link]] = {
    val nitfArticles = fetchNitfBundle().flatMap { articles =>
      Future.sequence(articles.zipWithIndex.map { case (article, index) =>
        downloadMainImage(article, index)
          .flatMap(writeToFile(_, index))
      })
    }

    val images = Await.ready(nitfArticles, querySettings.downloadTimeout)
      .map(_.flatMap(_.mainImage))

    val sections = nitfArticles.flatMap { articles =>
      Future.sequence(BookSection.fromArticles(articles).map(writeToFile))
    }
    val rootManifest = sections.flatMap(writeToFile)

    val linkables = List(nitfArticles, images, sections, rootManifest.map(Seq(_)))
    Future.reduceLeft(linkables)(_ ++ _)
      .map(_.map(_.link))
  }

  private def downloadMainImage(article: Article, fileNameIndex: Int): Future[Article] = {
    val image = article.mainImage
    if (publishingSettings.downloadImages && image.isDefined) {
      querier.downloadImage(image.get)
        .flatMap(writeToFile(_, fileNameIndex))
        .map(newLink => article.copy(mainImage = Some(newLink)))
    } else {
      Future.successful(article)  // with image sources as URLs
    }
  }

  private def writeToFile(image: ImageData, fileNameIndex: Int): Future[Image] = {
    val fileName = s"${fileNameIndex}_${image.metadata.id}.${fileExtension(image.source)}"
    writeToFile(image.data, fileName).map { newLink => image.metadata.copy(link = newLink) }
  }

  private def writeToFile(article: Article, fileNameIndex: Int): Future[Article] = {
    val nitfGenerator = ArticleNITF(article)
    val fileName = s"${fileNameIndex}_${asFileName(article.docId)}.${fileSettings.nitfExtension}"
    writeToFile(nitfGenerator.nitf, fileName).map { newLink => article.copy(link = newLink) }
  }

  private def writeToFile(bookSection: BookSection): Future[BookSection] = {
    val fileName = asFileName(bookSection.id) + "." + fileSettings.rssExtension
    val manifest = ArticlesManifest(bookSection)
    writeToFile(manifest.rss, fileName).map(bookSection.withLink)
  }

  private def writeToFile(sections: Seq[BookSection]): Future[RssManifest] = {
    val fileName = fileSettings.rootManifestFileName
    val manifest = SectionsManifest(
      title = publishingSettings.publicationName,
      link = Link.AbsoluteURL.from(publishingSettings.publicationLink),
      books = sections
    )
    writeToFile(manifest.rss, fileName).map { newLink => manifest.copy(link = newLink) }
  }

  private def writeToFile(content: Elem, fileName: String): Future[Link] = {
    val prettifier = if (publishingSettings.prettifyXml) defaultPrettyPrinter else TrimmingPrinter
    writeToFile(content.toXmlBytes(fileSettings.encoding)(prettifier), fileName)
  }

  private def writeToFile(content: Array[Byte], fileName: String): Future[Link] = Future {
    val path = Files.write(outputDirectory.resolve(fileName), content)
    val relativePath = outputDirectory.relativize(path)
    Link.RelativePath.from(relativePath, outputDirLink)
  }
}
