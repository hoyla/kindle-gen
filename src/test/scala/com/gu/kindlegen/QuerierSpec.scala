package com.gu.kindlegen

import java.time.LocalDate

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.SpanSugar._

import com.gu.contentapi.client.model.v1.{Content, SearchResponse}
import com.gu.contentapi.client.model.v1.TagType.NewspaperBook
import com.gu.contentapi.client.utils.CapiModelEnrichment._
import com.gu.kindlegen.DateUtils._

class QuerierSpec extends FlatSpec {
  val settings = Settings.load.get
  val querier = new Querier(settings, exampleDate.toOffsetDateTime.toLocalDate)

  val totalArticles = 96  // on exampleDate = 2017-07-24

  // TODO: Find a way to test printSentResponse, extract the edition dates etc
  // TODO: Find a way to override the source file to a sample.conf version

  val capiDate = exampleDate
  val testcontent = TestContent("", "", 3, "", "", capiDate, capiDate, capiDate, "", "", "", None).toContent
  val capiResponse = List(testcontent)
  val testArticle = TestContent("", "", 1, "", "", capiDate, capiDate, capiDate, "", "", "", None)

  ".responseToArticles" should "convert a capi response (Seq[Content) to a Seq[Article])" in {
    val toArticles = querier.responseToArticles(capiResponse)

    assert(toArticles.head.newspaperPageNumber === 3)
  }

  ".sortContentByPageAndSection" should "sort content according to page number then book section" in {

    val articles =
      Seq(
        ("theguardian/mainsection/topstories", 4),
        ("theguardian/mainsection/international", 1),
        ("theguardian/mainsection/finance", 1),
        ("theguardian/mainsection/international", 2),
        ("theguardian/mainsection/topstories", 3),
        ("theguardian/mainsection/international", 3)
      ).map {
          case (l, m) => testArticle.copy(testArticleNewspaperBook = l, testArticlePageNumber = m).toContent
      }.map(Article.apply)

    val mappedSortedContents: Seq[(String, Int)] = {
      Seq(
        ("theguardian/mainsection/finance", 1),
        ("theguardian/mainsection/international", 1),
        ("theguardian/mainsection/international", 2),
        ("theguardian/mainsection/international", 3),
        ("theguardian/mainsection/topstories", 3),
        ("theguardian/mainsection/topstories", 4)
      )
    }
    val sortedResults: Seq[(String, Int)] = querier.sortArticlesByPageAndSection(articles).map(article => (article.sectionId, article.newspaperPageNumber))
    assert(sortedResults == mappedSortedContents)
  }

  "fetchPrintSentResponse" should "initiate a proper search query" in {
    withFetchResponse() { _.total shouldEqual totalArticles }
  }

  "fetchPrintSentResponse" should "support empty responses" in {
    val holiday = LocalDate.of(2017, 12, 25)
    withFetchResponse(new Querier(settings, holiday)) { _.results should have length 0 }
  }

  "fetchPrintSentResponse" should "return all results - no pagination should be required" in {
    withFetchResponse() { response =>
      response.results should have length response.total
    }
  }

  "fetchPrintSentResponse" should "return unique results with no duplicates" in {
    withFetchResponse() { response =>
      val ids = response.results.map(_.id)
      val duplicateIds = ids.filter(id => ids.count(_ == id) > 1)
      withClue(s"${duplicateIds.length} duplicate ids found: $duplicateIds") {
        duplicateIds shouldBe empty
      }
    }
  }

  "responseToArticles" should "convert publishable content" in {
    querier.responseToArticles(Seq(testcontent)) should not be empty
  }

  "responseToArticles" should "ignore non-publishable content" in {
    val withoutTags = testcontent.copy(tags = Seq.empty)
    querier.responseToArticles(Seq(withoutTags)) shouldBe empty
  }

  private def withFetchResponse[T](querier: Querier = querier)(doSomething: SearchResponse => T): T = {
    whenReady(
      querier.fetchPrintSentResponse(),
      timeout(scaled(15.seconds)),
      interval(scaled(150.millis))
    )(doSomething)
  }
}
