package com.gu.kindlegen

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.util.Try
import scala.xml.Elem

import org.apache.logging.log4j.scala.Logging

import com.gu.io.{Downloader, Link, Publisher}
import com.gu.io.IOUtils._
import com.gu.io.Link.AbsoluteURL
import com.gu.xml._

object KindleGenerator {
  // some NITF tags must be minimised to be valid (e.g. <doc-id/> instead of <doc-id>\n</doc-id>)
  private val prettyPrinter = new PrettyPrinter(width = 150, step = 3, minimizeEmpty = true)
}

class KindleGenerator(provider: ArticlesProvider,
                      binder: BookBinder,
                      publisher: Publisher,
                      downloader: Downloader,
                      downloadTimeout: FiniteDuration,
                      publishingSettings: PublishingSettings)(implicit ec: ExecutionContext) extends Logging {
  logger.trace(s"Initialised with $publishingSettings")

  private def fileSettings = publishingSettings.files

  def fetchNitfBundle(): Future[Seq[Article]] = {
    provider.fetchArticles()
      .map { results =>
        val minArticles = publishingSettings.minArticlesPerEdition
        require(results.length >= minArticles,
          s"Not enough articles to generate an edition! Expected >= $minArticles, Found ${results.length}")
        results
      }
  }

  // this implementation fails if any of the operations fails
  // we might want to modify that so that failed operations, e.g. downloading an image, don't affect other operations
  def publish(): Future[Unit] = {
    for {
      articles <- fetchNitfBundle()
      eventualArticlesWithImages = Future.sequence(articles.zipWithIndex.map((downloadMainImage _).tupled))

      articlesWithImages <- Await.ready(eventualArticlesWithImages, downloadTimeout)  // force the timeout
      savedArticles <- Future.sequence(articlesWithImages.zipWithIndex.map((saveArticle _).tupled))
      savedSections <- Future.sequence(binder.group(savedArticles).map(saveSection))
      rootManifest <- saveRootManifest(savedSections)
      result <- publisher.publish()
    }
      yield result
  }

  private def downloadMainImage(article: Article, fileNameIndex: Int): Future[Article] = {
    article.mainImage match {
      case Some(image @ Image(_, AbsoluteURL(url), _, _, _)) if publishingSettings.downloadImages =>
        downloader.download(url)
          .map(ImageData(image, _))
          .flatMap(save(_, fileNameIndex))
          .map(newLink => article.copy(mainImage = Some(newLink)))
          .recover { case error =>
            logger.warn(s"Failed to download or save the image for article ${article.id}. Falling back to URL link.")
            article
          }
      case _ =>
        Future.successful(article)  // with image sources as URLs
    }
  }

  private def save(image: ImageData, fileNameIndex: Int): Future[Image] = {
    val fileName = s"${fileNameIndex}_${image.metadata.id}.${fileExtension(image.source)}"
    publisher.save(image.data, fileName).map { newLink => image.metadata.copy(link = newLink) }
  }

  private def saveArticle(article: Article, fileNameIndex: Int): Future[Article] = {
    val fileName = s"${fileNameIndex}_${asFileName(article.id)}.${fileSettings.nitfExtension}"
    logger.debug(s"Generating NITF for $fileName...")

    Future.fromTry(Try(ArticleNITF(article)))
      .flatMap { nitfGenerator =>
        saveXml(nitfGenerator.nitf, fileName).map { newLink => article.copy(link = newLink) }
      }
  }

  private def saveSection(bookSection: BookSection): Future[BookSection] = {
    val fileName = asFileName(bookSection.id) + "." + fileSettings.rssExtension
    logger.debug(s"Generating manifest for $fileName...")

    Future.fromTry(Try(ArticlesManifest(bookSection)))
      .flatMap { manifest =>
        saveXml(manifest.rss, fileName).map(bookSection.withLink)
      }
  }

  private def saveRootManifest(sections: Seq[BookSection]): Future[RssManifest] = {
    val fileName = fileSettings.rootManifestFileName
    logger.debug(s"Generating manifest for $fileName...")

    Future.fromTry(Try(SectionsManifest(
      title = publishingSettings.publicationName,
      link = Link.AbsoluteURL.from(publishingSettings.publicationLink),
      books = sections
    ))).flatMap { manifest =>
        saveXml(manifest.rss, fileName).map { newLink => manifest.copy(link = newLink) }
      }
  }

  private def saveXml(content: Elem, fileName: String): Future[Link] = {
    val prettifier = if (publishingSettings.prettifyXml) KindleGenerator.prettyPrinter else TrimmingPrinter
    publisher.save(content.toXmlBytes(fileSettings.encoding)(prettifier), fileName)
  }
}
