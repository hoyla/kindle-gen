package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.amazonaws.services.lambda.runtime.Context
import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.client.utils.CapiModelEnrichment
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class LambdaSuite extends FunSuite {

  import Lambda._

  test("string take") {
    val message = "hello, world"
    assert(message.take(5) === "hello")
  }

  // TODO: Find a way to override the source file to a sample.conf version
  test("Querier - readApiKey") {
    //    override val configSource = Source.fromFile("sample.conf")
    assert(Querier.readApiKey !== "test")
  }

  // TODO: Find a way to test printSentResponse, extract the edition dates etc

  // TODO: Find a way to test resultToArticle

  test("Querier.toManifest") {
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

  val test_article_list = {
    List(
      Article(
        newspaperBookSection = "theguardian/mainsection/international",
        sectionName = "International",
        newspaperPageNumber = 1,
        title = "my title",
        docId = "section/date/title",
        issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        byline = "my name",
        articleAbstract = "article abstract",
        content = "content"
      ),
      Article(
        newspaperBookSection = "theguardian/mainsection/international",
        sectionName = "International",
        newspaperPageNumber = 1,
        title = "my title",
        docId = "section/date/title",
        issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        byline = "my name",
        articleAbstract = "article abstract",
        content = "content"
      ),
      Article(
        newspaperBookSection = "theguardian/mainsection/international",
        sectionName = "International",
        newspaperPageNumber = 2,
        title = "my title",
        docId = "section/date/title",
        issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        byline = "my name",
        articleAbstract = "article abstract",
        content = "content"
      ),
      Article(
        newspaperBookSection = "theguardian/mainsection/international",
        sectionName = "International",
        newspaperPageNumber = 2,
        title = "my title",
        docId = "section/date/title",
        issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        byline = "my name",
        articleAbstract = "article abstract",
        content = "content"
      ),
      Article(
        newspaperBookSection = "theguardian/mainsection/international",
        sectionName = "International",
        newspaperPageNumber = 4,
        title = "my title",
        docId = "section/date/title",
        issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        byline = "my name",
        articleAbstract = "article abstract",
        content = "content"
      ),
      Article(
        newspaperBookSection = "theguardian/mainsection/international",
        sectionName = "International",
        newspaperPageNumber = 5,
        title = "my title",
        docId = "section/date/title",
        issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        byline = "my name",
        articleAbstract = "article abstract",
        content = "content"
      ),
      Article(
        newspaperBookSection = "theguardian/mainsection/international",
        sectionName = "International",
        newspaperPageNumber = 5,
        title = "my title",
        docId = "section/date/title",
        issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        byline = "my name",
        articleAbstract = "article abstract",
        content = "content"
      )
    )
  }

  val expectedOutBSP = List(
    BookSectionPage(
      bookSectionId = "International",
      pageNum = 1,
      articles =
        List(
          Article(
            newspaperBookSection = "theguardian/mainsection/international",
            sectionName = "International",
            newspaperPageNumber = 1,
            title = "my title",
            docId = "section/date/title",
            issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            byline = "my name",
            articleAbstract = "article abstract",
            content = "content"
          ),
          Article(
            newspaperBookSection = "theguardian/mainsection/international",
            sectionName = "International",
            newspaperPageNumber = 1,
            title = "my title",
            docId = "section/date/title",
            issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            byline = "my name",
            articleAbstract = "article abstract",
            content = "content"
          )
        )
    ),
    BookSectionPage(
      bookSectionId = "International",
      pageNum = 2,
      articles =
        List(
          Article(
            newspaperBookSection = "theguardian/mainsection/international",
            sectionName = "International",
            newspaperPageNumber = 2,
            title = "my title",
            docId = "section/date/title",
            issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            byline = "my name",
            articleAbstract = "article abstract",
            content = "content"
          ),
          Article(
            newspaperBookSection = "theguardian/mainsection/international",
            sectionName = "International",
            newspaperPageNumber = 2,
            title = "my title",
            docId = "section/date/title",
            issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            byline = "my name",
            articleAbstract = "article abstract",
            content = "content"
          )
        )
    ),
    BookSectionPage(
      bookSectionId = "International",
      pageNum = 4,
      articles =
        List(
          Article(
            newspaperBookSection = "theguardian/mainsection/international",
            sectionName = "International",
            newspaperPageNumber = 4,
            title = "my title",
            docId = "section/date/title",
            issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            byline = "my name",
            articleAbstract = "article abstract",
            content = "content"
          )
        )
    ),
    BookSectionPage(
      bookSectionId = "International",
      pageNum = 5,
      articles =
        List(
          Article(
            newspaperBookSection = "theguardian/mainsection/international",
            sectionName = "International",
            newspaperPageNumber = 5,
            title = "my title",
            docId = "section/date/title",
            issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            byline = "my name",
            articleAbstract = "article abstract",
            content = "content"
          ),
          Article(
            newspaperBookSection = "theguardian/mainsection/international",
            sectionName = "International",
            newspaperPageNumber = 5,
            title = "my title",
            docId = "section/date/title",
            issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            byline = "my name",
            articleAbstract = "article abstract",
            content = "content"
          )
        )
    )
  )
  val expectedOutBSP1 = List(
    BookSectionPage(
      bookSectionId = "International",
      pageNum = 1,
      articles =
        List(
          Article(
            newspaperBookSection = "theguardian/mainsection/international",
            sectionName = "International",
            newspaperPageNumber = 1,
            title = "my title",
            docId = "section/date/title",
            issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
            byline = "my name",
            articleAbstract = "article abstract",
            content = "content"
          )
        )
    )
  )

  val expectedOut = List(
    List(
      Article(
        newspaperBookSection = "theguardian/mainsection/international",
        sectionName = "International",
        newspaperPageNumber = 1,
        title = "my title",
        docId = "section/date/title",
        issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        byline = "my name",
        articleAbstract = "article abstract",
        content = "content"
      ),
      Article(
        newspaperBookSection = "theguardian/mainsection/international",
        sectionName = "International",
        newspaperPageNumber = 1,
        title = "my title",
        docId = "section/date/title",
        issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        byline = "my name",
        articleAbstract = "article abstract",
        content = "content"
      )
    ),
    List(
      Article(
        newspaperBookSection = "theguardian/mainsection/international",
        sectionName = "International",
        newspaperPageNumber = 2,
        title = "my title",
        docId = "section/date/title",
        issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        byline = "my name",
        articleAbstract = "article abstract",
        content = "content"
      ),
      Article(
        newspaperBookSection = "theguardian/mainsection/international",
        sectionName = "International",
        newspaperPageNumber = 2,
        title = "my title",
        docId = "section/date/title",
        issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        byline = "my name",
        articleAbstract = "article abstract",
        content = "content"
      )
    ),
    List(
      Article(
        newspaperBookSection = "theguardian/mainsection/international",
        sectionName = "International",
        newspaperPageNumber = 4,
        title = "my title",
        docId = "section/date/title",
        issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        byline = "my name",
        articleAbstract = "article abstract",
        content = "content"
      )
    ),
    List(
      Article(
        newspaperBookSection = "theguardian/mainsection/international",
        sectionName = "International",
        newspaperPageNumber = 5,
        title = "my title",
        docId = "section/date/title",
        issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        byline = "my name",
        articleAbstract = "article abstract",
        content = "content"
      ),
      Article(
        newspaperBookSection = "theguardian/mainsection/international",
        sectionName = "International",
        newspaperPageNumber = 5,
        title = "my title",
        docId = "section/date/title",
        issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
        byline = "my name",
        articleAbstract = "article abstract",
        content = "content"
      )
    )
  )

  val sortedArticles = test_article_list.sortBy(_.newspaperPageNumber)

  test(".toBookSectionPageList") {
    assert(Querier.toBookSectionPageList(sortedArticles) === expectedOutBSP)
  }

  test(".toBookSectionPageList with empty list") {
    assert(Querier.toBookSectionPageList(List()) === List())
  }

  test(".toBookSectionPageList with one item in list") {
    assert(Querier.toBookSectionPageList(List(sortedArticles.head)) === expectedOutBSP1)
  }

}

