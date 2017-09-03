package com.gu.kindlegen

import com.gu.contentapi.client.utils.CapiModelEnrichment
import org.scalatest.FlatSpec
import DateUtils._

class QuerierSpec extends FlatSpec {

  // TODO: Find a way to override the source file to a sample.conf version
  ".readApiKey" should "read API key from external conf file" in {
    assert(Querier.readApiKey !== "test")
  }

  // TODO: Find a way to test printSentResponse, extract the edition dates etc
  val capiDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime
  val testcontent = TestContent("", "", 3, "", "", capiDate, capiDate, capiDate, "", "", "", None).toContent
  val capiResponse = List(testcontent)

  ".responseToArticles" should "convert a capi response (Seq[Content) to a Seq[Article]" in {
    val toArticles = Querier.responseToArticles(capiResponse)
    assert(toArticles.head.newspaperPageNumber === 3)
  }

}
