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
  private lazy val outputDirectory: Path = Files.createDirectories(publishingSettings.outputDir).toRealPath()
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

    val articlesOnDisk = Await.result(fArticlesOnDisk, 5.minutes)  // TODO move the waiting time to configuration

    val sections = BookSection.fromArticles(articlesOnDisk).map(writeToFile)
    val rootManifest = writeToFile(SectionsManifest.apply(
      title = publishingSettings.publicationName,
      link = Link.AbsolutePath.from(publishingSettings.outputDir.toRealPath()),
      sections = sections
    ))

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
    val fileName = s"${fileNameIndex}_${image.metadata.id}.${image.fileExtension}"
    image.metadata.copy(link = writeToFile(image.data, fileName))
  }

  private def writeToFile(article: Article, fileNameIndex: Int): Article = {
    val nitf = ArticleNITF(article)
    val fileName = s"${fileNameIndex}_${asFileName(article.docId)}.nitf"
    article.copy(link = writeToFile(nitf.fileContents, fileName))
  }

  private def writeToFile(bookSection: BookSection): BookSection = {
    val fileName = asFileName(bookSection.id) + ".xml"
    val manifest = ArticlesManifest(bookSection).toManifestContentsPage
    bookSection.copy(section = bookSection.section.copy(link = writeToFile(manifest, fileName)))
  }

  private def writeToFile(sectionsManifest: SectionsManifest): SectionsManifest = {
    val fileName = "hierarchical-title-manifest.xml"
    val manifest = sectionsManifest.toManifestContentsPage
    sectionsManifest.copy(link = writeToFile(manifest, fileName))
  }

  private def writeToFile(content: String, fileName: String): Link.RelativePath =
    writeToFile(content.trim.getBytes("UTF-8"), fileName)

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
