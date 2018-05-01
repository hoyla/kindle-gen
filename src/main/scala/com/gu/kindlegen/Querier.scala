package com.gu.kindlegen

import java.time.LocalDate

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

import com.gu.contentapi.client._
import com.gu.contentapi.client.model.v1.{Content, SearchResponse}
import com.gu.io.IOUtils._
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
              editionDate: LocalDate)(implicit ec: ExecutionContext) {
  def fetchAllArticles(): Future[Seq[Article]] =
    fetchPrintSentResponse().andThen {
      case Success(response) =>
        // TODO log an error; exceptions in the call to `andThen` do not stop processing
        assert(response.results.length == response.total, "fetchResponse returned partial (paginated) results!")
    }.map { response =>
      articles(response.results)
    }

  def fetchPrintSentResponse(): Future[SearchResponse] = {
    val query = KindlePublishingSearchQuery(editionDate, responseTagTypes = Seq(settings.sectionTagType))
    capiClient.getResponse(query)
  }

  def articles(results: Seq[Content]): Seq[Article] = {
    val articles = results.map { content => Try(Article(content, settings.sectionTagType)) }.collect {
      case Success(article) => article
      // TODO log the issue in the case of failure
    }
    articles
  }

  def downloadArticleImage(article: Article): Future[Option[ImageData]] = {
    Future.traverse(article.mainImage.toList) { image =>
      download(image.link.source).map(bytes => ImageData(image, bytes))
    }.map(_.headOption)
  }
}

// TODO: create a fileStructure model class with paths and file names.
