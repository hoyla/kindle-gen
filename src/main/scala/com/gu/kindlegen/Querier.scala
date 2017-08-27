package com.gu.kindlegen

import com.gu.contentapi.client._
import com.gu.contentapi.client.model._
import org.joda.time.DateTime
import org.joda.time.format._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io.{ BufferedSource, Source }

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
    def formatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    //    def editionDateTime: DateTime = DateTime.now
    def editionDateTime: DateTime = formatter.parseDateTime("2017-05-19") // to have a date I know the results for
    def editionDateString: String = formatter.print(editionDateTime)

    def editionDateStart: DateTime = DateTime.parse(editionDateString).withMillisOfDay(0).withMillisOfSecond(0)

    def editionDateEnd: DateTime = DateTime.parse(editionDateString).withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59).withMillisOfSecond(999)

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
    //.showElements("image")
    // TODO: Add error handling with Try for failed request.
    // TODO: Currently gets one result only; separate out sections and map over multiple pageSizes/ pages
    // TODO: Query requires use-date and exact date of publication
    val response = Await.result(capiClient.getResponse(query), 5.seconds)
    response.results
  }

  def resultToArticles(response: Seq[com.gu.contentapi.client.model.v1.Content]): Seq[Article] = {
    response.map(responseContent =>
      Article(responseContent))
  }

  def toSectionHeading(articles: Seq[Article]): Seq[SectionHeading] = {
    articles.map(x =>
      SectionHeading(
        title = x.sectionName,
        titleLink = x.newspaperBookSection + ".xml"
      ))
  }

  // Pass in Querier.resultToArticles(getPrintSentResponse)
  def toManifest(articles: Seq[Article], buildDate: DateTime = DateTime.now): SectionManifest = {
    SectionManifest(
      publicationDate = articles.head.pubDate,
      buildDate = buildDate,
      sections = Querier.toSectionHeading(articles)
    )
  }
}

