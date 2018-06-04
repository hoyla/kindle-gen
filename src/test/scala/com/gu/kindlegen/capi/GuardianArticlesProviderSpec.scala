package com.gu.kindlegen.capi

import java.time.LocalDate

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

import com.gu.contentapi.client.model.v1.SearchResponse
import com.gu.io.sttp.OkHttpSttpDownloader
import com.gu.kindlegen.{Settings, TestContent}
import com.gu.kindlegen.TestContent._

class GuardianArticlesProviderSpec extends FlatSpec with ScalaFutures with IntegrationPatience {
  private val settings = Settings.load.get.copy(provider = ExampleGuardianProviderSettings)

  private val downloader = OkHttpSttpDownloader()
  private def provider: GuardianArticlesProvider = provider(ExampleOffsetDate.toLocalDate)
  private def provider(editionDate: LocalDate) = GuardianArticlesProvider(settings, downloader, editionDate)

  val totalArticles = 96  // on exampleDate = 2017-07-24

  val capiDate = ExampleDate
  val testContent = TestContent("", "", 3, "", "", capiDate, capiDate, capiDate, "", "", "", None)
  val capiResponse = List(testContent.toContent)

  "articles" should "convert a capi response (Seq[Content) to a Seq[Article])" in {
    val articles = provider.articles(capiResponse)

    assert(articles.nonEmpty)
    assert(articles.head.newspaperPageNumber === 3)
  }

  "articles" should "convert publishable content" in {
    provider.articles(Seq(testContent.toContent)) should not be empty
  }

  "articles" should "ignore non-publishable content" in {
    val withoutTags = testContent.toContent.copy(tags = Seq.empty)
    provider.articles(Seq(withoutTags)) shouldBe empty
  }

  "fetchPrintSentResponse" should "initiate a proper search query" in {
    withFetchResponse() { _.total shouldEqual totalArticles }
  }

  "fetchPrintSentResponse" should "support empty responses" in {
    val holiday = LocalDate.of(2017, 12, 25)
    withFetchResponse(provider(holiday)) { _.results should have length 0 }
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

  private def withFetchResponse[T](querier: GuardianArticlesProvider = provider)(doSomething: SearchResponse => T): T = {
    whenReady(querier.fetchPrintSentResponse())(doSomething)
  }
}
