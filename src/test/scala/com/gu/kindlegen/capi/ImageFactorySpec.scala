package com.gu.kindlegen.capi

import org.scalatest.FunSpec
import org.scalatest.Matchers._
import org.scalatest.OptionValues._

import com.gu.contentapi.client.model.v1.{AssetFields, Content}
import com.gu.kindlegen.capi.TestContent._


class ImageFactorySpec extends FunSpec {
  describe("reading from Content") {
    val factory = new ImageFactory(ExampleGuardianProviderSettings)

    val testContent = TestContent.Sample

    it("extracts fields") {
      val image = factory.mainImage(testContent.toContent).value

      image.id should not be empty
      image.altText.value shouldBe "altText"
      image.caption.value shouldBe "caption"
      image.credit.value shouldBe "credit"
      image.link.source shouldBe testContent.imageUrl.value
    }

    val maxResolution = ExampleGuardianProviderSettings.maxImageResolution
    Map(
      "small" -> (maxResolution - 1),
      "medium" -> maxResolution,
      "large" -> (maxResolution + 1)
    ).foreach { case (size, resolution) =>
      val isAcceptable = resolution <= maxResolution

      Map(
        "width" -> withWidth(resolution),
        "height" -> withHeight(resolution)
      ).foreach { case (field, setter) =>
        val actions = if (isAcceptable) "picks" else "skips"

        it(s"$actions a $size image using $field") {
          val content = setter(testContent.toContent)
          val maybeImage = factory.mainImage(content)

          if (resolution <= maxResolution)
            maybeImage shouldBe defined
          else
            maybeImage shouldBe empty
        }
      }
    }
  }

  private def withWidth(w: Int): Content => Content = withAssetFields(_, _.copy(width = Some(w)))
  private def withHeight(h: Int): Content => Content = withAssetFields(_, _.copy(height = Some(h)))

  private def withAssetFields(content: Content, newAssetFields: AssetFields => AssetFields): Content = {
    content.copy(elements = content.elements.map(_.map(element =>
      element.copy(assets = element.assets.map(asset => asset.copy(typeData = asset.typeData.map(newAssetFields))))
    )))
  }
}
