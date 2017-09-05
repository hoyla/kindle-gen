package com.gu.kindlegen
import java.awt.image.BufferedImage
import java.io._
import javax.imageio.ImageIO

import com.gu.contentapi.client._
import com.gu.contentapi.client.model._
import com.gu.contentapi.client.model.v1.Content

import scala.concurrent.Await
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
      // TODO: what fields are required? main? content? Is tag `newspaper-book` required
      .showTags("newspaper-book-section, newspaper-book")
      .showElements("image")
    // TODO: Add error handling with Try for failed request.
    // TODO: Currently gets one result only; separate out sections and map over multiple pageSizes/ pages
    // TODO: Query requires use-date and exact date of publication
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

  // TODO: wrap Futures around image request
  def fetchImageData(articles: List[Article]): Unit = {
    articles.map {
      article =>
        {
          val urlOption = article.imageUrl
          urlOption.flatMap(getImageData)
        }
    }
  }

  // TODO: look up try monad instead of try catch
  // TODO: the e Exception thing isn't correct - look up
  def getImageData(url: String): Option[Array[Byte]] =
    try {
      val response: HttpRequest = Http(url)
      Some(response.asBytes.body)
    } catch {
      case e: Exception => None
    }
}