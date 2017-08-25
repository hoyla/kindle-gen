package com.gu.kindlegen

import com.amazonaws.services.lambda.runtime.Context
import com.gu.contentapi.client._
import com.gu.contentapi.client.model._
import com.gu.contentapi.client.model.v1.TagType.NewspaperBookSection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import scala.io.BufferedSource
import org.joda.time.DateTime
import org.joda.time.format._
import scala.io.Source

/**
 * This is compatible with aws' lambda JSON to POJO conversion.
 * You can test your lambda by sending it the following payload:
 * {"name": "Bob"}
 */
class LambdaInput() {
  var name: String = _
  def getName(): String = name
  def setName(theName: String): Unit = name = theName
}

case class Env(app: String, stack: String, stage: String) {
  override def toString: String = s"App: $app, Stack: $stack, Stage: $stage\n"
}

object Env {
  def apply(): Env = Env(
    Option(System.getenv("App")).getOrElse("DEV"),
    Option(System.getenv("Stack")).getOrElse("DEV"),
    Option(System.getenv("Stage")).getOrElse("DEV")
  )
}

object Lambda {

  /*
   * This is your lambda entry point
   */
  def handler(lambdaInput: LambdaInput, context: Context): Unit = {
    val logger = context.getLogger
    val env = Env()
    logger.log(s"Starting $env")
    logger.log(process(lambdaInput.name, env))
  }

  /*
   * I recommend to put your logic outside of the handler
   */
  def process(name: String, env: Env): String = s"Hello $name! (from ${env.app} in ${env.stack})\n"
}

object TestIt {
  def main(args: Array[String]): Unit = {
    println(Lambda.process(args.headOption.getOrElse("Alex"), Env()))
  }
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

  def toBookSectionPageList(articles: List[Article]): List[BookSectionPage] = {
    if (articles.isEmpty) { return List() }
    val initial = (List[BookSectionPage](), List[Article]())
    val sortedchunks4 = articles.foldLeft(initial) { (acc, elem) =>
      acc match {
        case (Nil, Nil) => {
          (List(), List(elem))
        }
        case (Nil, y) => {
          if (y.headOption.map(_.newspaperPageNumber).contains(elem.newspaperPageNumber)) {
            (List(), (elem :: y))
          } else {
            val bsp = BookSectionPage(
              bookSectionId = y.head.newspaperBookSection,
              pageNum = y.head.newspaperPageNumber,
              articles = y
            )
            (List(bsp), List(elem))
          }
        }
        case (x :: xs, Nil) => {
          ((x :: xs), List(elem))
        }
        case ((x :: xs), y) => {
          if (y.headOption.map(_.newspaperPageNumber).contains(elem.newspaperPageNumber)) {
            ((x :: xs), (elem :: y))
          } else {
            val bsp = BookSectionPage(
              bookSectionId = y.head.newspaperBookSection,
              pageNum = y.head.newspaperPageNumber,
              articles = y
            )
            (bsp :: (x :: xs), elem :: Nil)
          }
        }
      }
    }
    val lastPageArticles = sortedchunks4._2
    val lastBookSectionPage = BookSectionPage(
      bookSectionId = lastPageArticles.head.newspaperBookSection,
      pageNum = lastPageArticles.head.newspaperPageNumber,
      articles = lastPageArticles
    )
    val r = (lastBookSectionPage :: sortedchunks4._1).reverse
      .map(x => Tuple2(x.articles.length, List(x.articles.map(_.newspaperPageNumber))))
    //    println(r)
    (lastBookSectionPage :: sortedchunks4._1).reverse
  }

  def toBookSectionList(bookSectionPages: List[BookSectionPage]): List[BookSection] = {
    if (bookSectionPages.isEmpty) { return List() }
    val initial: (List[BookSection], List[BookSectionPage]) = (List(), List())
    val chunkedPages = bookSectionPages.foldLeft(initial) { (acc, elem) =>
      acc match {
        case (Nil, Nil) => {
          (List(), List(elem))
        }
        case (Nil, y) => {
          if (y.head.bookSectionId == elem.bookSectionId) {
            (List(), elem :: y)
          } else {
            val bs = BookSection(
              bookSectionId = y.head.bookSectionId,
              bookSectionTitle = y.head.articles.head.sectionName,
              pages = y.reverse
            )
            (List(bs), elem :: Nil)
          }
        }
        case (x :: xs, Nil) => {
          ((x :: xs), List(elem))
        }
        case (x :: xs, y) => {
          if (y.head.bookSectionId == elem.bookSectionId) {
            (List(), elem :: y)
          } else {
            val bs = BookSection(
              bookSectionId = y.head.bookSectionId,
              bookSectionTitle = y.head.articles.head.sectionName,
              pages = y.reverse
            )
            (List(bs), elem :: Nil)
          }
        }
      }
    }
    val lastBookSectionPages = chunkedPages._2
    val lastBookSection = BookSection(
      bookSectionId = lastBookSectionPages.head.bookSectionId,
      bookSectionTitle = lastBookSectionPages.head.articles.head.sectionName,
      pages = lastBookSectionPages.reverse
    )
    val r = (lastBookSection :: chunkedPages._1).reverse
      .map(x => Tuple2(x.pages.length, List(x.pages.map(_.bookSectionId))))
    println(r)
    (lastBookSection :: chunkedPages._1).reverse
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

