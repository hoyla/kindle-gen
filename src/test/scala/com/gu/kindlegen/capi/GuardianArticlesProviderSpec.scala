package com.gu.kindlegen.capi

import java.time.LocalDate

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.OptionValues._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

import com.gu.contentapi.client.model.v1.{SearchResponse, TagType}
import com.gu.io.sttp.OkHttpSttpDownloader
import com.gu.kindlegen.capi.TestContent._
import com.gu.kindlegen.TestData._
import com.gu.kindlegen.app.Settings

class GuardianArticlesProviderSpec extends FlatSpec with ScalaFutures with IntegrationPatience {
  private val settings = Settings.load.get.copy(articles = ExampleGuardianProviderSettings)

  private val downloader = OkHttpSttpDownloader()
  private def provider: GuardianArticlesProvider = provider(ExampleDate.toLocalDate)
  private def provider(editionDate: LocalDate) =
    GuardianArticlesProvider(settings.contentApi, settings.articles, downloader, editionDate)

  val totalArticles = 96  // on exampleDate = 2017-07-24
  val testContent = TestContent.Sample.toContent

  "articles" should "convert publishable content" in {
    val articles = provider.articles(Seq(testContent))
    articles should have size 1
    articles.headOption.value.id shouldBe testContent.id
  }

  "articles" should "ignore non-publishable content" in {
    val withoutTags = testContent.copy(tags = Seq.empty)
    val withOtherTags = testContent.copy(tags = Seq(tag("tag2", tagType = TagType.Tracking)))
    provider.articles(Seq(withoutTags, withOtherTags)) shouldBe empty
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
