package com.gu.kindlegen.capi

import scala.concurrent.duration._

import com.gu.contentapi.client.model.v1._
import com.gu.contentapi.client.utils.CapiModelEnrichment._
import com.gu.kindlegen.TestData._


object TestContent {
  val ExampleGuardianProviderSettings = GuardianProviderSettings(1.minute, TagType.NewspaperBookSection, maxImageResolution = 1000)

  val Sample =
    TestContent("sample-article", "Sample Article", "sample-section", 0, ExampleDate.toCapiDateTime,
      "sample byline", "sample stand1st", Some("https://example.com/sample-image.jpg"))

  def tag(id: String, tagType: TagType = ExampleGuardianProviderSettings.sectionTagType) = Tag(
    id = id,
    webTitle = id.capitalize,
    `type` = tagType,
    webUrl = s"https://theguardian.com/$id",
    apiUrl = s"https://content.guardianapis.com/$id"
  )

  def element(id: String, elementType: ElementType = ElementType.Image, assets: Seq[Asset]): Element = Element(
    id = id,
    `type` = elementType,
    relation = "main",
    assets = assets
  )

  def asset(url: String, assetType: AssetType = AssetType.Image): Asset = Asset(
    `type` = assetType,
    typeData = Some(AssetFields(
      secureFile = Some(url),
      altText = Some("altText"),
      caption = Some("caption"),
      credit = Some("credit"),
      height = Some(ExampleGuardianProviderSettings.maxImageResolution),
      width = Some(ExampleGuardianProviderSettings.maxImageResolution),
    ))
  )
}

case class TestContent(id: String,
                       title: String,
                       sectionId: String,
                       pageNumber: Int,
                       issueDate: CapiDateTime,
                       byline: String,
                       standFirst: String,
                       imageUrl: Option[String]) {

  def toContent: Content = Content(
    id = id,
    `type` = ContentType.Article,
    tags = List(sectionTag),
    apiUrl = s"${sectionTag.apiUrl}/$id",
    webUrl = s"https://theguardian.com/${sectionTag.webUrl}/$id",
    webTitle = title,
    elements = imageElement.map(Seq(_)),
    fields = Option(ContentFields(
      newspaperEditionDate = Option(issueDate),
      standfirst = Option(standFirst),
      headline = Option(title),
      newspaperPageNumber = Option(pageNumber),
      byline = Option(byline),
    )),
  )

  import TestContent._

  val sectionTag: Tag = tag(sectionId)

  val imageElement: Option[Element] = imageUrl.map { url =>
    element(id = url, assets = Seq(asset(url)))
  }
}
