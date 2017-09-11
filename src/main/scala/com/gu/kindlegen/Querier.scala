package com.gu.kindlegen

import com.gu.contentapi.client._
import com.gu.contentapi.client.model._
import com.gu.contentapi.client.model.v1.{ Content, SearchResponse }

import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success }
import scala.concurrent.duration._
import scala.io.{ BufferedSource, Source }
import scala.concurrent.ExecutionContext.Implicits.global
import scalaj.http._
import DateUtils._
import com.gu.contentapi.client.model.v1.TagType.NewspaperBookSection

// TODO: Move elsewhere
case class ArticleImage(articleId: Int, data: Array[Byte])

class Querier {
}

object Querier {

  def readConfig(lineNum: Int): String = {
    // local file `~/.gu/kindle-gen.conf` must exist with first line a valid API key for CAPI. Second line targetUrl
    val localUserHome: String = scala.util.Properties.userHome
    val configSource: BufferedSource = Source.fromFile(s"$localUserHome/.gu/kindle-gen.conf")
    val arr = configSource.getLines.toArray
    arr(lineNum)
  }

  class PrintSentContentClient(override val apiKey: String) extends GuardianContentClient(apiKey) {

    override val targetUrl: String = readConfig(1)
  }

  val readApiKey: String = readConfig(0)

  def getPrintSentResponse(pageNum: Int): SearchResponse = {

    val capiKey = readApiKey
    val capiClient = new PrintSentContentClient(capiKey)
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
    response.sortBy(content => (content.fields.flatMap(_.newspaperPageNumber), content.tags.find(_.`type` == NewspaperBookSection).get.id))
  }
  // TODO: handle the possibility of there being no content in the getPrintSentResponse method above
  // TODO: This isn't to do with querying the API so we should move it somewhere else.
  def responseToArticles(response: Seq[Content]): Seq[Article] = {
    val sortedContent = Querier.sortContentByPageAndSection(response)
    val contentWithIndex = sortedContent.view.zipWithIndex.toList
    contentWithIndex.map(Article.apply)
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
