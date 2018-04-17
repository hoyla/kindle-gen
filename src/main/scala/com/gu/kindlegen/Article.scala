package com.gu.kindlegen

import com.gu.contentapi.client.model.v1.Asset
import com.gu.contentapi.client.model.v1.Element
import com.gu.contentapi.client.model.v1.ElementType.Image
import com.gu.contentapi.client.model.v1.TagType.NewspaperBookSection
import com.gu.contentapi.client.model.v1._

case class Article(
    newspaperBookSection: String,
    sectionName: String,
    newspaperPageNumber: Int,
    title: String,
    docId: String,
    issueDate: CapiDateTime,
    releaseDate: CapiDateTime,
    pubDate: CapiDateTime,
    byline: String,
    articleAbstract: String,
    content: String,
    imageUrl: Option[String],
    fileId: Int
) {
  // TODO: TEST THIS
  def fileName: String = {
    val titleDashFormatted = title.replace(" ", "-")
    s"${fileId}_$titleDashFormatted.nitf"
  }
}

object Article {
  def getImageUrl(content: Content): Option[String] = {
    def isMainImage(e: Element): Boolean =
      e.`type` == Image && e.relation == "main"
    def isCorrectSizedAsset(a: Asset): Boolean =
      a.typeData.flatMap(_.width) == Some(500)

    val elements = content.elements
    val mainImage = elements.flatMap(_.find(isMainImage))
    val imageAsset = mainImage.flatMap(_.assets.find(isCorrectSizedAsset))
    // keep imageUrl as an option otherwise later you will try to download form an empty string
    val imageUrl = imageAsset.flatMap(_.typeData).flatMap(_.secureFile)
    imageUrl
  }

  def getBodyHtml(content: Content): String = {

    def isTypeText(e: BlockElement): Boolean =
      e.`type` == ElementType.Text

    val blocks = content.blocks
    val bodyBlocks: Seq[Block] = blocks.flatMap(_.body).getOrElse(Nil)
    val bodyBlocksElements: Seq[BlockElement] = bodyBlocks.flatMap(_.elements)
    val textElemField: Seq[String] = bodyBlocksElements.flatMap(_.textTypeData.flatMap(_.html))
    val noOpt = textElemField.mkString
    noOpt
  }

  def apply(content: Content, index: Int): Article = {
    val maybeSectionTag = content.tags.find(_.`type` == NewspaperBookSection)
    val maybeNewspaperDate = content.fields.flatMap(_.newspaperEditionDate).orElse(content.webPublicationDate)

    val contentId = s"Content(${content.id})"
    require(content.fields.nonEmpty, s"$contentId retrieved without fields")
    require(maybeSectionTag.nonEmpty, s"$contentId doesn't have a NewspaperBookSection")
    require(maybeNewspaperDate.nonEmpty, s"$contentId doesn't have a NewspaperEditionDate")

    apply(content, index, maybeNewspaperDate.get, content.fields.get, maybeSectionTag.get)
  }

  def apply(content: Content, index: Int, newspaperDate: CapiDateTime, fields: ContentFields, sectionTag: Tag): Article = {
    Article(
      newspaperBookSection = sectionTag.id,
      sectionName = sectionTag.webTitle,
      newspaperPageNumber = fields.newspaperPageNumber.getOrElse(0),
      title = content.webTitle,
      docId = content.id,
      issueDate = newspaperDate,
      releaseDate = newspaperDate,
      pubDate = newspaperDate,
      byline = fields.byline.getOrElse(""),
      articleAbstract = fields.standfirst.getOrElse(""),
      content = getBodyHtml(content),
      imageUrl = getImageUrl(content),
      fileId = index
    )
  }
}
