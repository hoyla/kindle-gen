package com.gu.kindlegen

import java.time.OffsetDateTime

import com.gu.contentapi.client.model.v1._
import com.gu.contentapi.client.utils.CapiModelEnrichment._
import com.gu.io.{Link, Linkable}


case class Article(
    section: Section,
    newspaperPageNumber: Int,
    title: String,
    docId: String,
    link: Link,
    pubDate: OffsetDateTime,
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

  def apply(content: Content, settings: GuardianProviderSettings): Article = {
    val sectionTagType = settings.sectionTagType
    val maybeSectionTag = content.tags.find(_.`type` == sectionTagType)
    val maybeNewspaperDate = content.fields.flatMap(_.newspaperEditionDate)

    val contentId = s"""Content(id="${content.id}")"""
    require(content.fields.nonEmpty, s"$contentId retrieved without fields")
    require(maybeSectionTag.nonEmpty, s"$contentId doesn't have a $sectionTagType")
    require(maybeNewspaperDate.nonEmpty, s"$contentId doesn't have a NewspaperEditionDate")

    apply(content, maybeNewspaperDate.get, content.fields.get, maybeSectionTag.get, settings)
  }

  def apply(content: Content,
            newspaperDate: CapiDateTime,
            fields: ContentFields,
            sectionTag: Tag,
            settings: GuardianProviderSettings): Article = {
    Article(
      section = Section.from(sectionTag),
      newspaperPageNumber = fields.newspaperPageNumber.getOrElse(Int.MaxValue),  // move to the end of the section
      title = content.webTitle,
      docId = content.id,
      link = Link.AbsoluteURL.from(content.webUrl),
      pubDate = newspaperDate.toOffsetDateTime,
      byline = fields.byline.getOrElse(""),
      articleAbstract = fields.standfirst.getOrElse(""),
      // TODO handle non-text articles (e.g. cartoons -> stand1st)
      bodyBlocks = getBodyBlocks(content),
      mainImage = Image.mainImage(content, settings)
    )
  }
}
