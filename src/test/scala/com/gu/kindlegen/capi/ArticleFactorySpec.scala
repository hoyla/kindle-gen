package com.gu.kindlegen.capi

import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSpec
import org.scalatest.Matchers._

import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.client.utils.CapiModelEnrichment._
import com.gu.kindlegen.Image
import com.gu.kindlegen.TestData._
import com.gu.kindlegen.capi.TestContent._


class ArticleFactorySpec extends FunSpec with MockFactory {
  private val settings: GuardianProviderSettings = ExampleGuardianProviderSettings

  describe("reading from Content") {
    val testContent = TestContent.Sample
    lazy val factory = new ArticleFactory(settings, stub[ImageFactory])
    lazy val article = factory(testContent.toContent)

    it("extracts simple fields") {
      article.id shouldBe testContent.id
      article.title shouldBe testContent.title
      article.byline shouldBe testContent.byline
      article.articleAbstract shouldBe testContent.standFirst
      article.pageNumber shouldBe testContent.pageNumber
      article.pubDate shouldBe testContent.issueDate.toOffsetDateTime
    }

    it("extracts section information") {
      article.section.id shouldBe testContent.sectionTag.id
      article.section.title shouldBe testContent.sectionTag.webTitle
      article.section.link.source shouldBe testContent.sectionTag.webUrl
    }
  }

  describe("with images") {
    val contentWithTrailText = TestContent.Sample.toContent.adjustFields(_.copy(trailText = Some("trailText")))

    def factories: (ImageFactory, ArticleFactory) = {
      val imageFactory = mock[ImageFactory]
      (imageFactory, new ArticleFactory(settings, imageFactory))
    }

    it("attempts to read the image") {
      val (imageFactory, articleFactory) = factories

      imageFactory.mainImage _ expects (*, None)
      articleFactory(contentWithTrailText)
    }

    describe("when processing cartoons") {
      val contentWithoutCaption = contentWithTrailText.adjustAssetFields(_.copy(caption = None))

      settings.cartoonTags.foreach { tag =>
        describe(s"with tag ${tag.id}") {
          def cartoon(content: Content) = content.copy(tags = content.tags :+ tag)

          it("uses trail text for the caption fallback") {
            val (imageFactory, articleFactory) = factories

            imageFactory.mainImage _ expects (*, Some("trailText"))
            articleFactory(cartoon(contentWithoutCaption))
          }
        }
      }
    }
  }
}
