package com.gu.kindlegen

import com.gu.contentapi.client._
import com.gu.contentapi.client.model._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io.{ BufferedSource, Source }
import org.joda.time.DateTime
import DateUtils._

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

  def getPrintSentResponse: Seq[com.gu.contentapi.client.model.v1.Content] = {

    val capiKey = readApiKey
    val capiClient = new PrintSentContentClient(capiKey)
    val pageNum = 1
    val query = SearchQuery()
      .pageSize(10)
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
  // TODO: with show-elements=image in the request get an elemnet sublist of images including a sublist of assets which have type = image and a typedata sublist including width - only want the width 500 image
  // The Kindle NITF spec is for max image size https://images-na.ssl-images-amazon.com/images/G/01/kindle-publication/feedGuide-new/KPPUserGuide._V181169266_.html#AddingImages
  // max size is 960 x 720 corresps with our 500 width best
  // in the typedata there is a link called secureFile and this is where I want to retrieve the image from (or maybe just the topsection `file`
  // as well as the fingerpost file, kindleprevier also gets these files in an async way - how?
  // best plan is probably to add all the functionality in terms of section and class and do the async download later

  def responseToArticles(response: Seq[com.gu.contentapi.client.model.v1.Content]): Seq[Article] = {
    response.map(responseContent =>
      Article(responseContent))
  }

}

