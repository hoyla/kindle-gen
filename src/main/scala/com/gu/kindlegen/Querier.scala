package com.gu.kindlegen

import java.time.Instant
import java.time.temporal.ChronoUnit.{DAYS, NANOS}

import com.gu.contentapi.client._
import com.gu.contentapi.client.model._
import com.gu.contentapi.client.model.v1.TagType.NewspaperBookSection
import com.gu.contentapi.client.model.v1.{ Content, SearchResponse }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scalaj.http._

// TODO: Move elsewhere
case class ArticleImage(articleId: Int, data: Array[Byte])

object Querier {
  // TODO: change this to DateTime.now or function that takes a passed in date.
  private def editionDateTime: Instant = Instant.parse("2017-05-19T00:00:00Z") // to have a date I know the results for
  //  def editionDateTime: DateTime = DateTime.now()

  def editionDateStart: Instant = editionDateTime
  def editionDateEnd: Instant = editionDateTime.plus(1, DAYS).minus(1, NANOS)

  class PrintSentContentClient(settings: Settings) extends GuardianContentClient(settings.contentApiKey) {

    override val targetUrl: String = settings.contentApiTargetUrl
  }
}

class Querier(settings: Settings) {
  import Querier._

  def getPrintSentResponse(pageNum: Int): SearchResponse = {

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
    contentWithIndex.map { case (content, index) => Article(content, index) }
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

  // TODO: look up try monad instead of try catch
  // TODO: the e Exception
  def getImageData(url: String): Future[Array[Byte]] = Future {
    val response: HttpRequest = Http(url)
    response.asBytes.body
  }
}

// TODO: create a fileStructure model class with paths and file names.
