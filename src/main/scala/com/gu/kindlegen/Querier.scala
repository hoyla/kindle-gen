package com.gu.kindlegen

import java.time.LocalDate

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import org.apache.logging.log4j.scala.Logging

import com.gu.contentapi.client._
import com.gu.contentapi.client.model.v1.{Content, SearchResponse}
import com.gu.io.IOUtils._
import com.gu.kindlegen.KindlePublishingSearchQuery.MaxResultsPageSize
import com.gu.kindlegen.Querier.PrintSentContentClient


// TODO: Move elsewhere
case class ImageData(metadata: Image, data: Array[Byte]) {
  def source: String = metadata.link.source
}

object Querier {
  class PrintSentContentClient(settings: ContentApiSettings) extends GuardianContentClient(settings.apiKey) {
    override val targetUrl: String = settings.targetUrl
  }
}

class Querier(capiClient: PrintSentContentClient,
              settings: QuerySettings,
              editionDate: LocalDate)(implicit ec: ExecutionContext) extends Logging {
  logger.trace(s"Initialised to query ${capiClient.targetUrl} with $settings")

  def fetchAllArticles(): Future[Seq[Article]] =
    fetchPrintSentResponse()
      .andThen(reportQueryErrors)
      .map(response => articles(response.results))

  def fetchPrintSentResponse(): Future[SearchResponse] = {
    val query = KindlePublishingSearchQuery(editionDate, responseTagTypes = Seq(settings.sectionTagType))
    logger.debug(s"Querying CAPI: ${capiClient.url(query)}")
    capiClient.getResponse(query)
  }

  def articles(results: Seq[Content]): Seq[Article] = {
    val articles = results.flatMap(toArticle)
    logger.info(s"Processed ${articles.length} articles.")
    articles
  }

  private def toArticle(content: Content): Option[Article] = {
    val id = content.id
    logger.info(s"Processing content $id")
    logger.trace(s"Processing content $id: $content")

    Try(Article(content, settings)) match {
      case Success(article) =>
        logger.trace(s"Processed content $id into $article")
        Some(article)
      case Failure(error) =>
        logger.warn(s"Failed to process content $id!", error)
        logger.debug(s"Failed to process content $id: $content")
        None
    }
  }

  def downloadImage(image: Image): Future[ImageData] = {
    val link = image.link
    logger.info(s"Downloading image from $link")

    download(link.source)
      .map(bytes => ImageData(image, bytes))
      .andThen {
        case Failure(error) => logger.error(s"Failed to download image from $link!", error)
      }
  }

  private def reportQueryErrors: PartialFunction[Try[SearchResponse], Unit] = {
    case Success(response) =>
      if (response.results.length != response.total)
        logger.warn(
          s"The CAPI query returned paginated results, despite asking for $MaxResultsPageSize!" +
            s" Expected ${response.total} but found ${response.results.length}.")
    case Failure(error) =>
      logger.fatal("Failed to query CAPI!", error)
  }
}
