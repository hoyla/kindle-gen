package com.gu.kindlegen

import com.gu.contentapi.client.model.v1._


case class Article(
    sectionId: String,
    sectionName: String,
    newspaperPageNumber: Int,
    title: String,
    docId: String,
    pubDate: CapiDateTime,
    byline: String,
    articleAbstract: String,
    bodyBlocks: Seq[String],
    imageUrl: Option[String]
) {
  // TODO: TEST THIS
  // TODO move this method to another class (ArticleNITF?)
  def fileName: String = {
    docId.replace('/', '_') + ".nitf"
  }
}

object Article {
  def getImageUrl(content: Content): Option[String] = {
    def isMainImage(e: Element): Boolean =
      e.`type` == ElementType.Image && e.relation == "main"

    def isMasterImage(a: Asset): Boolean =
      a.typeData.flatMap(_.isMaster).getOrElse(false)

    val elements = content.elements.getOrElse(Nil)
    val mainImage = elements.find(isMainImage)
    val imageAsset = mainImage.flatMap(_.assets.find(isMasterImage))
    // keep imageUrl as an option otherwise later you will try to download form an empty string
    val imageUrl = imageAsset.flatMap(_.typeData).flatMap(_.secureFile)
    imageUrl
  }

  def getBodyBlocks(content: Content): Seq[String] = {
    val blocks = content.blocks
    val bodyBlocks = blocks.flatMap(_.body).getOrElse(Nil)
    // TODO what if bodyBlocks.length < blocks.totalBodyBlocks ?
    val bodyElements = bodyBlocks.flatMap(_.elements).filter(_.`type` == ElementType.Text)
    val htmlBlocks = bodyElements.flatMap(_.textTypeData.flatMap(_.html))
    htmlBlocks
  }

  def apply(content: Content): Article = {
    val maybeSectionTag = content.tags.find(_.`type` == TagType.NewspaperBook)
    val maybeNewspaperDate = content.fields.flatMap(_.newspaperEditionDate)

    val contentId = s"""Content(id="${content.id}")"""
    require(content.fields.nonEmpty, s"$contentId retrieved without fields")
    require(maybeSectionTag.nonEmpty, s"$contentId doesn't have a NewspaperBook")
    require(maybeNewspaperDate.nonEmpty, s"$contentId doesn't have a NewspaperEditionDate")

    apply(content, maybeNewspaperDate.get, content.fields.get, maybeSectionTag.get)
  }

  def apply(content: Content, newspaperDate: CapiDateTime, fields: ContentFields, sectionTag: Tag): Article = {
    Article(
      sectionId = sectionTag.id,
      sectionName = sectionTag.webTitle,
      newspaperPageNumber = fields.newspaperPageNumber.getOrElse(0),
      title = content.webTitle,
      docId = content.id,
      pubDate = newspaperDate,
      byline = fields.byline.getOrElse(""),
      articleAbstract = fields.standfirst.getOrElse(""),
      // TODO handle non-text articles (e.g. cartoons)
      bodyBlocks = getBodyBlocks(content),
      imageUrl = getImageUrl(content)
    )
  }
}
