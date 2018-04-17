package com.gu.kindlegen

import java.time.Instant
import java.time.temporal.ChronoUnit.{DAYS, NANOS}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Success, Try}

import scalaj.http._

import com.gu.contentapi.client._
import com.gu.contentapi.client.model._
import com.gu.contentapi.client.model.v1.{Content, SearchResponse}
import com.gu.contentapi.client.model.v1.TagType.NewspaperBookSection

// TODO: Move elsewhere
case class ArticleImage(articleId: Int, data: Array[Byte])

object Querier {
  class PrintSentContentClient(settings: Settings) extends GuardianContentClient(settings.contentApiKey) {

    override val targetUrl: String = settings.contentApiTargetUrl
  }
}

class Querier(settings: Settings, editionDateTime: Instant)(implicit ec: ExecutionContext) {
  import Querier._

  def getPrintSentResponse(pageNum: Int): SearchResponse = {
    val editionDateStart = editionDateTime.truncatedTo(DAYS)
    val editionDateEnd = editionDateStart.plus(1, DAYS).minus(1, NANOS)

    val capiClient = new PrintSentContentClient(settings)
    val query = SearchQuery()
      .pageSize(5)
      .showFields("all") //TODO: Don't need all fields.
      .orderBy("newest")
      .fromDate(editionDateStart)
      .toDate(editionDateEnd)
      .useDate("newspaper-edition")
      .page(pageNum)
      .showFields("newspaper-page-number, headline,newspaper-edition-date,byline,standfirst,body")
      .showTags("newspaper-book-section, newspaper-book")
      .showElements("image")
      .showBlocks("all")
    // TODO: Add error handling with Try for failed request.
    // TODO: Await is blocking - takes ages! One day is 26 pages.
    val response = Await.result(capiClient.getResponse(query), 5.seconds)
    response
  }

  def getAllPagesContent: List[SearchResponse] = {
    def paginatedResponses(currentPageNumber: Int = 1, accumulatedPages: List[SearchResponse] = Nil): List[SearchResponse] = {
      val currentPageContent = getPrintSentResponse(currentPageNumber)
      val accumulatedAndCurrent = currentPageContent :: accumulatedPages
      if (currentPageContent.currentPage >= currentPageContent.pages) {
        accumulatedAndCurrent
      } else {
        paginatedResponses(currentPageNumber + 1, accumulatedAndCurrent)
      }
    }
    paginatedResponses()
  }

  def responsesToContent(responses: List[SearchResponse]): Seq[Content] = {
    responses.flatten(_.results)
  }

  def sortContentByPageAndSection(response: Seq[Content]): Seq[Content] = {
    response.sortBy(content => (content.fields.flatMap(_.newspaperPageNumber), content.tags.find(_.`type` == NewspaperBookSection).map(_.id)))
  }
  // TODO: handle the possibility of there being no content in the getPrintSentResponse method above
  // TODO: This isn't to do with querying the API so we should move it somewhere else.
  def responseToArticles(response: Seq[Content]): Seq[Article] = {
    val sortedContent = sortContentByPageAndSection(response)
    val contentWithIndex = sortedContent.view.zipWithIndex.toList
    contentWithIndex.map { case (content, index) => Try(Article(content, index)) }.collect {
      case Success(article) => article
    }
  }

  // This will probably only be called only when sending the files to Amazon because the images can be stored in memory until they are written to the ftp server. Will use the Article.fileID to name the retrieved image byte[].

  def getAllArticleImages(articles: Seq[Article]): Future[Seq[ArticleImage]] = {
    val futures = articles.flatMap(getArticleImage)
    Future.sequence(futures)
  }

  def getArticleImage(article: Article): Option[Future[ArticleImage]] = {
    article.imageUrl.map(url => {
      val future = getImageData(url)
      future.map(bytes => ArticleImage(articleId = article.fileId, data = bytes))
    })
  }

  def getImageData(url: String): Future[Array[Byte]] = Future {
    val response: HttpRequest = Http(url)
    response.asBytes.body
  }
}

// TODO: create a fileStructure model class with paths and file names.
