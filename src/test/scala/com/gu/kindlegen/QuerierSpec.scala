package com.gu.kindlegen

import java.time.{Instant, LocalDate}

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.client.model.v1.TagType.NewspaperBookSection
import com.gu.contentapi.client.utils.CapiModelEnrichment._
import com.gu.kindlegen.DateUtils._

class QuerierSpec extends FlatSpec {
  val settings = Settings.load.get
  val querier = new Querier(settings, exampleDate.toOffsetDateTime.toLocalDate)

  val totalArticles = 96  // on exampleDate = 2017-07-24

  // TODO: Find a way to test printSentResponse, extract the edition dates etc
  // TODO: Find a way to override the source file to a sample.conf version

  val capiDate = exampleDate
  val testcontent = TestContent("", "", 3, "", "", capiDate, capiDate, capiDate, "", "", "", None, 0).toContent
  val capiResponse = List(testcontent)
  val testArticle = TestContent("", "", 1, "", "", capiDate, capiDate, capiDate, "", "", "", None, 0)

  ".responseToArticles" should "convert a capi response (Seq[Content) to a Seq[Article])" in {
    val toArticles = querier.responseToArticles(capiResponse)

    assert(toArticles.head.newspaperPageNumber === 3)
  }

  ".sortContentByPageAndSection" should "sort content according to page number then book section" in {

    val contents: Seq[Content] = {
      Seq(
        ("theguardian/mainsection/topstories", 4),
        ("theguardian/mainsection/international", 1),
        ("theguardian/mainsection/finance", 1),
        ("theguardian/mainsection/international", 2),
        ("theguardian/mainsection/topstories", 3),
        ("theguardian/mainsection/international", 3)
      ).map {

          case (l, m) => testArticle.copy(testArticleNewspaperBookSection = l, testArticlePageNumber = m).toContent
        }
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
    val mappedResult: Seq[(String, Int)] = querier.sortContentByPageAndSection(contents).map(content => (content.tags.find(_.`type` == NewspaperBookSection).get.id, content.fields.flatMap(_.newspaperPageNumber).get))
    println(contents.head)
    println(mappedResult)
    assert(mappedResult == mappedSortedContents)
  }

  "getPrintSentResponse" should "initiate a proper search query" in {
    querier.getPrintSentResponse(pageNum = 1).total shouldEqual totalArticles
  }

  "getAllPagesContent" should "handle empty responses" in {
    val holiday = LocalDate.of(2017, 12, 25)
    new Querier(settings, holiday).getAllPagesContent.flatMap(_.results) should have length 0
  }

  "getAllPagesContent" should "extract all response pages" in {
    querier.getAllPagesContent.flatMap(_.results) should have length totalArticles
  }

  "responseToArticles" should "convert publishable content" in {
    querier.responseToArticles(Seq(testcontent)) should not be empty
  }

  "responseToArticles" should "ignore non-publishable content" in {
    val withoutTags = testcontent.copy(tags = Seq.empty)
    querier.responseToArticles(Seq(withoutTags)) shouldBe empty
  }
}
