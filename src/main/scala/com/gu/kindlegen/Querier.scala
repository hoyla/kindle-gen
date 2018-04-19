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

  def fetchPrintSentResponse(): Future[SearchResponse] = {
    val capiClient = new PrintSentContentClient(settings)
    val query = KindlePublishingSearchQuery(editionDate)
    capiClient.getResponse(query)
  }

  def fetchAllContents(): Seq[Content] = (
    // TODO: Add error handling for failed request.
    // TODO: Await is blocking - takes ages!
    Await.result(fetchPrintSentResponse(), 5.seconds)
      ensuring(response => response.results.length == response.total, "fetchResponse returned partial (paginated) results!")
  ).results

  def sortArticlesByPageAndSection(articles: Seq[Article]): Seq[Article] = {
    articles.sortBy(article => (article.newspaperPageNumber, article.sectionId))
  }

  // TODO: handle the possibility of there being no content in the fetchPrintSentResponse method above
  // TODO: This isn't to do with querying the API so we should move it somewhere else.
  def responseToArticles(response: Seq[Content]): Seq[Article] = sortArticlesByPageAndSection(
    response.map { content => Try(Article(content)) }.collect {
      case Success(article) => article
      // TODO log the issue in the case of failure
    }
  )

  // This will probably only be called only when sending the files to Amazon because the images can be stored in memory until they are written to the ftp server. Will use the Article.fileID to name the retrieved image byte[].

  def getAllArticleImages(articles: Seq[Article]): Future[Seq[ImageData]] = {
    val futures = articles.flatMap(getArticleImage)
    Future.sequence(futures)
  }

  def getArticleImage(article: Article): Option[Future[ImageData]] = {
    article.mainImage.map { image =>
      download(image.url).map(bytes => ImageData(image, bytes))
    }
  }

  def download(url: String): Future[Array[Byte]] = Future {
    val response: HttpRequest = Http(url)
    response.asBytes.body
  }
}

// TODO: create a fileStructure model class with paths and file names.
