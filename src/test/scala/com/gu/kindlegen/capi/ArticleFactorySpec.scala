package com.gu.kindlegen.capi

import org.scalatest.FunSpec
import org.scalatest.Matchers._

import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.client.utils.CapiModelEnrichment._


class ArticleFactorySpec extends FunSpec {
  describe("reading from Content") {
    val factory = new ArticleFactory(TestContent.ExampleGuardianProviderSettings)
    def articleFrom(content: Content) = factory(content)

    val content = TestContent.Sample
    val article = articleFrom(content.toContent)

    it("extracts simple fields") {
      article.id shouldBe content.id
      article.title shouldBe content.title
      article.byline shouldBe content.byline
      article.articleAbstract shouldBe content.standFirst
      article.pageNumber shouldBe content.pageNumber
      article.pubDate shouldBe content.issueDate.toOffsetDateTime
    }

    it("extracts section information") {
      article.section.id shouldBe content.sectionTag.id
      article.section.title shouldBe content.sectionTag.webTitle
      article.section.link.source shouldBe content.sectionTag.webUrl
    }
  }
}
