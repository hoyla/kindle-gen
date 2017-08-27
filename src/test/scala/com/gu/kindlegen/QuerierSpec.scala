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

  ".toManifest" should "convert article to manifest contents page" in {
    val issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime
    val article = Article(
      newspaperBookSection = "theguardian/mainsection/international",
      sectionName = "International",
      0, "my title", "", issueDate, issueDate, issueDate, "my name", "article abstract", "content"
    )
    val articles = List(article)
    val time = DateTime.now()

    assert(Querier.toManifest(articles, time) === SectionManifest(
      publicationDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
      buildDate = time,
      sections = List(SectionHeading("International", "theguardian/mainsection/international.xml"))
    ))
  }

  // TODO: Find a way to test printSentResponse, extract the edition dates etc
  // TODO: Find a way to test resultToArticle

  private def formatter = DateTimeFormat.forPattern("yyyyMMdd")

}
