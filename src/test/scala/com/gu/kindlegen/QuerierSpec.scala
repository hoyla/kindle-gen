package com.gu.kindlegen

import java.time.LocalDate

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

import com.gu.contentapi.client.model.v1.SearchResponse
import com.gu.contentapi.client.utils.CapiModelEnrichment._
import com.gu.kindlegen.DateUtils._
import com.gu.kindlegen.Querier.PrintSentContentClient

class QuerierSpec extends FlatSpec with ScalaFutures with IntegrationPatience {
  val settings = Settings.load.get.contentApi
  val capiClient = new PrintSentContentClient(settings) // we can mock this for local testing
  val querier = new Querier(capiClient, exampleDate.toOffsetDateTime.toLocalDate)

  val totalArticles = 96  // on exampleDate = 2017-07-24

  // TODO: Find a way to test printSentResponse, extract the edition dates etc
  // TODO: Find a way to override the source file to a sample.conf version

  val capiDate = exampleDate
  val testContent = TestContent("", "", 3, "", "", capiDate, capiDate, capiDate, "", "", "", None)
  val capiResponse = List(testContent.toContent)

  ".responseToArticles" should "convert a capi response (Seq[Content) to a Seq[Article])" in {
    val toArticles = querier.sortedArticles(capiResponse)

    assert(toArticles.head.newspaperPageNumber === 3)
  }

  ".sortedArticles" should "sort content according to page number then book section" in {

    val articles =
      Seq(
        ("theguardian/mainsection/topstories", 4),
        ("theguardian/mainsection/international", 1),
        ("theguardian/mainsection/finance", 1),
        ("theguardian/mainsection/international", 2),
        ("theguardian/mainsection/topstories", 3),
        ("theguardian/mainsection/international", 3)
      ).map {
          case (l, m) => testContent.copy(testArticleNewspaperBook = l, testArticlePageNumber = m).toContent
      }

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
    val sortedResults: Seq[(String, Int)] = querier.sortedArticles(articles).map(article => (article.section.id, article.newspaperPageNumber))
    assert(sortedResults == mappedSortedContents)
  }

  "fetchPrintSentResponse" should "initiate a proper search query" in {
    withFetchResponse() { _.total shouldEqual totalArticles }
  }

  "fetchPrintSentResponse" should "support empty responses" in {
    val holiday = LocalDate.of(2017, 12, 25)
    withFetchResponse(new Querier(capiClient, holiday)) { _.results should have length 0 }
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

  "sortedArticles" should "convert publishable content" in {
    querier.sortedArticles(Seq(testContent.toContent)) should not be empty
  }

  "sortedArticles" should "ignore non-publishable content" in {
    val withoutTags = testContent.toContent.copy(tags = Seq.empty)
    querier.sortedArticles(Seq(withoutTags)) shouldBe empty
  }

  private def withFetchResponse[T](querier: Querier = querier)(doSomething: SearchResponse => T): T = {
    whenReady(querier.fetchPrintSentResponse())(doSomething)
  }
}
