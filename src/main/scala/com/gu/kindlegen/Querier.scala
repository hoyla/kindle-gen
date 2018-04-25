package com.gu.kindlegen

import java.time.LocalDate

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

import com.gu.contentapi.client._
import com.gu.contentapi.client.model.v1.{Content, SearchResponse}
import com.gu.io.IOUtils
import com.gu.io.IOUtils._


// TODO: Move elsewhere
case class ImageData(metadata: Image, data: Array[Byte]) {
  def fileExtension: String = IOUtils.fileExtension(metadata.link.source)
}

object Querier {
  class PrintSentContentClient(settings: ContentApiSettings) extends GuardianContentClient(settings.apiKey) {

    override val targetUrl: String = settings.targetUrl
  }
}

class Querier(settings: ContentApiSettings, editionDate: LocalDate)(implicit ec: ExecutionContext) {
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

  def sortedArticles(results: Seq[Content]): Seq[Article] = {
    val articles = results.map { content => Try(Article(content)) }.collect {
      case Success(article) => article
      // TODO log the issue in the case of failure
    }
    sortArticlesByPageAndSection(articles)
  }

  private def sortArticlesByPageAndSection(articles: Seq[Article]): Seq[Article] = {
    articles.sortBy(article => (article.newspaperPageNumber, article.section))
  }

  def downloadArticleImage(article: Article): Future[Option[ImageData]] = {
    Future.traverse(article.mainImage.toList) { image =>
      download(image.link.source).map(bytes => ImageData(image, bytes))
    }.map(_.headOption)
  }
}

// TODO: create a fileStructure model class with paths and file names.
