package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.gu.contentapi.client.utils.CapiModelEnrichment
import com.amazonaws.services.lambda.runtime.Context
import com.gu.contentapi.client.model.v1.Content
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class QuerierSpec extends FunSuite {

  // TODO: Find a way to override the source file to a sample.conf version
  test("Querier.readApiKey reads key from external file") {
    //    override val configSource = Source.fromFile("sample.conf")
    assert(Querier.readApiKey !== "test")
  }

  // TODO: Find a way to test printSentResponse, extract the edition dates etc
  // TODO: Find a way to test resultToArticle

  test("Querier.toManifest converts Article to Manifest contents page") {
    val article = Article(
      newspaperBookSection = "theguardian/mainsection/international",
      sectionName = "International",
      newspaperPageNumber = 0,
      title = "my title",
      docId = "section/date/title",
      issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
      releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
      pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
      byline = "my name",
      articleAbstract = "article abstract",
      content = "content"
    )
    val articles = List(article)
    val time = DateTime.now()

    assert(Querier.toManifest(articles, time) === SectionManifest(
      publicationDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
      buildDate = time,
      sections = List(SectionHeading("International", "theguardian/mainsection/international.xml"))
    ))
  }

  private def formatter = DateTimeFormat.forPattern("yyyyMMdd")

}
