package com.gu.kindlegen

import java.time.LocalDate

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

import com.gu.contentapi.client.model.v1.SearchResponse
import com.gu.contentapi.client.utils.CapiModelEnrichment._
import com.gu.kindlegen.Querier.PrintSentContentClient
import com.gu.kindlegen.TestContent._

class QuerierSpec extends FlatSpec with ScalaFutures with IntegrationPatience {
  val settings = Settings.load.get.contentApi
  val capiClient = new PrintSentContentClient(settings) // we can mock this for local testing

  private def querier: Querier = querier(ExampleOffsetDate.toLocalDate)
  private def querier(editionDate: LocalDate) = new Querier(capiClient, ExampleQuerySettings, editionDate)

  val totalArticles = 96  // on exampleDate = 2017-07-24

  // TODO: Find a way to test printSentResponse, extract the edition dates etc
  // TODO: Find a way to override the source file to a sample.conf version

  val capiDate = ExampleDate
  val testContent = TestContent("", "", 3, "", "", capiDate, capiDate, capiDate, "", "", "", None)
  val capiResponse = List(testContent.toContent)

  "articles" should "convert a capi response (Seq[Content) to a Seq[Article])" in {
    val articles = querier.articles(capiResponse)

    assert(articles.nonEmpty)
    assert(articles.head.newspaperPageNumber === 3)
  }

  "articles" should "convert publishable content" in {
    querier.articles(Seq(testContent.toContent)) should not be empty
  }

  "articles" should "ignore non-publishable content" in {
    val withoutTags = testContent.toContent.copy(tags = Seq.empty)
    querier.articles(Seq(withoutTags)) shouldBe empty
  }

  "fetchPrintSentResponse" should "initiate a proper search query" in {
    withFetchResponse() { _.total shouldEqual totalArticles }
  }

  "fetchPrintSentResponse" should "support empty responses" in {
    val holiday = LocalDate.of(2017, 12, 25)
    withFetchResponse(querier(holiday)) { _.results should have length 0 }
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

  private def withFetchResponse[T](querier: Querier = querier)(doSomething: SearchResponse => T): T = {
    whenReady(querier.fetchPrintSentResponse())(doSomething)
  }
}
