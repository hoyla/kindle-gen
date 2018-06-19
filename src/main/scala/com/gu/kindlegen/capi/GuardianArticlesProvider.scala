package com.gu.kindlegen.capi

import java.time.LocalDate

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import org.apache.logging.log4j.scala.Logging

import com.gu.contentapi.client.{AbstractContentApiClient, ContentApiClient, SttpContentApiClient}
import com.gu.contentapi.client.model.v1.{Content, SearchResponse}
import com.gu.io.sttp.SttpDownloader
import com.gu.kindlegen._


object GuardianArticlesProvider {
  def apply(credentials: ContentApiCredentials,
            capiSettings: GuardianProviderSettings,
            downloader: SttpDownloader,
            editionDate: LocalDate)(implicit ec: ExecutionContext): GuardianArticlesProvider = {
    val capiClient = contentApiClient(credentials, downloader)
    new GuardianArticlesProvider(capiClient, capiSettings, editionDate)
  }

  private def contentApiClient(credentials: ContentApiCredentials, sttpDownloader: SttpDownloader): ContentApiClient = {
    new AbstractContentApiClient(credentials.apiKey, Some(credentials.targetUrl)) with SttpContentApiClient {
      override protected def downloader = sttpDownloader
    }
  }
}

class GuardianArticlesProvider(capiClient: ContentApiClient,
                               settings: GuardianProviderSettings,
                               editionDate: LocalDate)(implicit ec: ExecutionContext)
    extends ArticlesProvider with Logging {

  logger.trace(s"Initialised to query ${capiClient.targetUrl} with $settings")

  def fetchArticles(): Future[Seq[Article]] =
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

  private def reportQueryErrors: PartialFunction[Try[SearchResponse], Unit] = {
    case Success(response) =>
      import KindlePublishingSearchQuery.MaxResultsPageSize

      if (response.results.length != response.total)
        logger.warn(
          s"The CAPI query returned paginated results, despite asking for $MaxResultsPageSize!" +
            s" Expected ${response.total} but found ${response.results.length}.")

    case Failure(error) =>
      logger.fatal("Failed to query CAPI!", error)
  }
}
