package com.gu.kindlegen.capi

import org.apache.logging.log4j.scala.Logging

import com.gu.contentapi.client.model.v1._
import com.gu.contentapi.client.utils.CapiModelEnrichment._
import com.gu.io.Link
import com.gu.io.Link.AbsoluteURL
import com.gu.kindlegen.{Section, _}


object ArticleFactory {
  /** Content fields processed by this factory
    * @see [[com.gu.contentapi.client.model.v1.Content.fields]]
    */
  val ContentFields = Set("byline", "newspaper-edition-date", "newspaper-page-number", "standfirst", "trailText")

  /** Content blocks processed by this factory
    * @see [[com.gu.contentapi.client.model.v1.Content.blocks]]
    */
  val Blocks = Set("body")

  /** Content element types processed by this factory
    * @see [[com.gu.contentapi.client.model.v1.Content.elements]]
    */
  val ElementTypes = Set[ElementType](ElementType.Image)

  private def tag(id: String, tagType: TagType): Tag =
    Tag(id, tagType, webTitle = "", webUrl = "", apiUrl = "")

  // TODO move to settings
  private[capi] val cartoonTags = Set(
    tag("type/cartoon", TagType.Type),
    tag("tone/cartoons", TagType.Tone),
    tag("commentisfree/series/guardian-comment-cartoon", TagType.Series),
    tag("commentisfree/series/observer-comment-cartoon", TagType.Series),
  )

  private[capi] val cartoonTagIds = cartoonTags.map(_.id)
}

class ArticleFactory(settings: GuardianProviderSettings, imageFactory: ImageFactory) extends Logging {
  import com.gu.kindlegen.capi.ArticleFactory._

  def apply(content: Content): Article = {
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
    val isCartoon = content.tags.map(_.id).exists(cartoonTagIds)
    if (isCartoon)
      logger.debug(s"Detected cartoon in ${content.id}")

    val captionFallback = if (isCartoon) content.fields.flatMap(_.trailText) else None
    val mainImage = imageFactory.mainImage(content, captionFallback)

    Article(
      id = content.id,
      title = content.webTitle,
      section = sectionFrom(sectionTag),
      pageNumber = fields.newspaperPageNumber.getOrElse(Int.MaxValue),  // move to the end of the section
      link = Link.AbsoluteURL.from(content.webUrl),
      pubDate = newspaperDate.toOffsetDateTime,
      byline = fields.byline.getOrElse(""),
      articleAbstract = fields.standfirst.getOrElse(""),
      bodyBlocks = getBodyBlocks(content),
      mainImage = mainImage
    )
  }

  private def getBodyBlocks(content: Content): Seq[String] = {
    val blocks = content.blocks
    val bodyBlocks = blocks.flatMap(_.body).getOrElse(Nil)
    val bodyElements = bodyBlocks.flatMap(_.elements).filter(_.`type` == ElementType.Text)
    val htmlBlocks = bodyElements.flatMap(_.textTypeData.flatMap(_.html))
    htmlBlocks
  }

  private def sectionFrom(tag: Tag): Section =
    Section(tag.id, tag.webTitle, AbsoluteURL.from(tag.webUrl))
}
