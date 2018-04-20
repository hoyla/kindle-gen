package com.gu.kindlegen

import java.time.LocalDate

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Success, Try}

import scalaj.http._

import com.gu.contentapi.client._
import com.gu.contentapi.client.model.v1.{Content, SearchResponse}


// TODO: Move elsewhere
case class ImageData(metadata: Image, data: Array[Byte]) {
  def fileExtension: String = {
    val url = metadata.url
    url.substring(url.lastIndexOf('.') + 1)
  }
}

object Querier {
  class PrintSentContentClient(settings: Settings) extends GuardianContentClient(settings.contentApiKey) {

    override val targetUrl: String = settings.contentApiTargetUrl
  }
}

class Querier(settings: Settings, editionDate: LocalDate)(implicit ec: ExecutionContext) {
  import Querier._

  def fetchAllArticles(): Future[Seq[Article]] =
    fetchPrintSentResponse().andThen {
      case Success(response) =>
        // TODO log an error; exceptions in the call to `andThen` do not stop processing
        assert(response.results.length == response.total, "fetchResponse returned partial (paginated) results!")
    }.map { response =>
      sortedArticles(response.results)
    }

  def fetchPrintSentResponse(): Future[SearchResponse] = {
    val capiClient = new PrintSentContentClient(settings)
    val query = KindlePublishingSearchQuery(editionDate)
    capiClient.getResponse(query)
  }

  def sortArticlesByPageAndSection(articles: Seq[Article]): Seq[Article] = {
    articles.sortBy(article => (article.newspaperPageNumber, article.sectionId))
  }

  def sortedArticles(results: Seq[Content]): Seq[Article] = sortArticlesByPageAndSection(
    results.map { content => Try(Article(content)) }.collect {
      case Success(article) => article
      // TODO log the issue in the case of failure
    }
  )

  def downloadArticleImage(article: Article): Future[Option[ImageData]] = {
    Future.traverse(article.mainImage.toList) { image =>
      download(image.url).map(bytes => ImageData(image, bytes))
    }.map(_.headOption)
  }

  def download(url: String): Future[Array[Byte]] = Future {
    val response: HttpRequest = Http(url)
    response.asBytes.body
  }
}

// TODO: create a fileStructure model class with paths and file names.
