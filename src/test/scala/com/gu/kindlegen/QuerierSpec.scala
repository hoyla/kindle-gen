package com.gu.kindlegen

import com.gu.contentapi.client.utils.CapiModelEnrichment
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import org.scalatest.FlatSpec

class QuerierSpec extends FlatSpec {

  // TODO: Find a way to override the source file to a sample.conf version
  ".readApiKey" should "read API key from external conf file" in {
    assert(Querier.readApiKey !== "test")
  }

  // TODO: Find a way to test printSentResponse, extract the edition dates etc
  def formatter = DateTimeFormat.forPattern("yyyyMMdd")
  val capiDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime
  val testcontent = TestContent("", "", 3, "", "", capiDate, capiDate, capiDate, "", "", "").toContent
  val capiResponse = List(testcontent)

  ".responseToArticles" should "convert a capi response (Seq[Content) to a Seq[Article]" in {
    val toArticles = Querier.responseToArticles(capiResponse)
    assert(toArticles.head.newspaperPageNumber === 3)
  }

}
