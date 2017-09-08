package com.gu.kindlegen

import com.gu.contentapi.client._
import com.gu.contentapi.client.model._
import com.gu.contentapi.client.model.v1.Content

import scala.concurrent.{ Future, Await }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success }
import scala.concurrent.duration._
import scala.io.{ BufferedSource, Source }
import scala.concurrent.ExecutionContext.Implicits.global
import scalaj.http._
import DateUtils._
import com.gu.contentapi.client.model.v1.TagType.NewspaperBookSection

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

  def getPrintSentResponse: Seq[Content] = {

    val capiKey = readApiKey
    val capiClient = new PrintSentContentClient(capiKey)
    val pageNum = 1
    val query = SearchQuery()
      .pageSize(5)
      .showFields("all")
      .orderBy("newest")
      .fromDate(editionDateStart)
      .toDate(editionDateEnd)
      .useDate("newspaper-edition")
      //      .page(pageNum)
      .showFields("newspaper-page-number, headline,newspaper-edition-date,byline,standfirst,body")
      .showTags("newspaper-book-section, newspaper-book")
      .showElements("image")
    // TODO: Add error handling with Try for failed request.
    // TODO: Currently gets one page of results only; separate out sections and map over multiple pageSizes/ pages
    // TODO: Query requires use-date and exact date of publication
    // TODO: Add pagination
    // TODO: Await is blocking - structure code so as the pages are retrieved the images can also strat to be requested
    val response = Await.result(capiClient.getResponse(query), 5.seconds)
    response.results
  }

  // TODO: handle the possibility of there being no content in the getPrintSentResponse method above
  def responseToArticles(response: Seq[Content]): Seq[Article] = {
    val sortedContent = Querier.sortContentByPageAndSection(response)
    val contentWithIndex = sortedContent.view.zipWithIndex.toList
    contentWithIndex.map(Article.apply)
  }

  def sortContentByPageAndSection(response: Seq[Content]): Seq[Content] = {
    response.sortBy(content => (content.fields.flatMap(_.newspaperPageNumber), content.tags.find(_.`type` == NewspaperBookSection).get.id))
  }

  // This will probably only be called only when sending the files to Amazon because the images can be stored in memory until they are written to the ftp server. Will use the Article.fileID to name the retrieved image byte[].
  def fetchImageData(articles: List[Article]): List[Future[Option[Array[Byte]]]] = {
    articles.map {
      article =>
        {
          val urlOption = article.imageUrl
          urlOption match {
            case Some(url) => Querier.getImageData(url)
            case None => Future.successful(None)
          }
        }
    }
  }

  //  // TODO: look up try monad instead of try catch
  //  // TODO: the e Exception
  def getImageData(url: String): Future[Option[Array[Byte]]] = Future {
    try {
      val response: HttpRequest = Http(url)
      Some(response.asBytes.body)
    } catch {
      case e: Exception => None
    }
  }
}

// TODO: create a fileStructure model class with paths and file names.
