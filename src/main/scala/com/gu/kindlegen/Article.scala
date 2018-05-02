package com.gu.kindlegen

import com.gu.contentapi.client.model.v1._


case class Article(
    section: Section,
    newspaperPageNumber: Int,
    title: String,
    docId: String,
    link: Link,
    pubDate: CapiDateTime,
    byline: String,
    articleAbstract: String,
    bodyBlocks: Seq[String],
    mainImage: Option[Image]) extends Linkable

object Article {
  def getBodyBlocks(content: Content): Seq[String] = {
    val blocks = content.blocks
    val bodyBlocks = blocks.flatMap(_.body).getOrElse(Nil)
    val bodyElements = bodyBlocks.flatMap(_.elements).filter(_.`type` == ElementType.Text)
    val htmlBlocks = bodyElements.flatMap(_.textTypeData.flatMap(_.html))
    htmlBlocks
  }

  def apply(content: Content, sectionTagType: TagType): Article = {
    val maybeSectionTag = content.tags.find(_.`type` == sectionTagType)
    val maybeNewspaperDate = content.fields.flatMap(_.newspaperEditionDate)

    val contentId = s"""Content(id="${content.id}")"""
    require(content.fields.nonEmpty, s"$contentId retrieved without fields")
    require(maybeSectionTag.nonEmpty, s"$contentId doesn't have a $sectionTagType")
    require(maybeNewspaperDate.nonEmpty, s"$contentId doesn't have a NewspaperEditionDate")

    apply(content, maybeNewspaperDate.get, content.fields.get, maybeSectionTag.get)
  }

  def apply(content: Content, newspaperDate: CapiDateTime, fields: ContentFields, sectionTag: Tag): Article = {
    Article(
      section = Section(sectionTag),
      newspaperPageNumber = fields.newspaperPageNumber.getOrElse(Int.MaxValue),  // move to the end of the section
      title = content.webTitle,
      docId = content.id,
      link = Link.AbsoluteURL.from(content.webUrl),
      pubDate = newspaperDate,
      byline = fields.byline.getOrElse(""),
      articleAbstract = fields.standfirst.getOrElse(""),
      // TODO handle non-text articles (e.g. cartoons)
      bodyBlocks = getBodyBlocks(content),
      mainImage = Image.mainMaster(content)
    )
  }
}
