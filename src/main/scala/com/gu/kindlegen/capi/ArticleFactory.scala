package com.gu.kindlegen.capi

import org.apache.logging.log4j.scala.Logging
import org.jsoup.Jsoup

import com.gu.contentapi.client.model.v1._
import com.gu.contentapi.client.model.v1.ElementType._
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
}

class ArticleFactory(settings: GuardianProviderSettings, imageFactory: ImageFactory) extends Logging {
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

    val bodyBlocks = getBodyBlocks(content).map(_.trim)
    val byline = fields.byline.map(_.trim).filter(_.nonEmpty).orElse {
      if (isCartoon || bodyBlocks.forall(_.isEmpty)) mainImage.flatMap(_.credit)
      else None
    }

    Article(
      id = content.id,
      title = content.webTitle,
      section = sectionFrom(sectionTag),
      pageNumber = fields.newspaperPageNumber.getOrElse(Int.MaxValue),  // move to the end of the section
      link = Link.AbsoluteURL.from(content.webUrl),
      pubDate = newspaperDate.toOffsetDateTime,
      byline = byline.getOrElse(""),
      articleAbstract = fields.standfirst.getOrElse(""),
      bodyBlocks = bodyBlocks,
      mainImage = mainImage
    )
  }

  private def getBodyBlocks(content: Content): Seq[String] = {
    val blocks = content.blocks
    val bodyBlocks = blocks.flatMap(_.body).getOrElse(Nil)
    val htmlBlocks = bodyBlocks.flatMap(_.elements).collect {
      case element if element.`type` == Text => element.textTypeData.flatMap(_.html)
      case element if element.`type` == Tweet => element.tweetTypeData.flatMap(_.html).map(h => addSpacesAround(emphasiseTweet(h)))
    }
    // sadly, starting from the second block, the first paragraph of each block is not indented
    // to work around this, we combine all blocks into one.
    val combinedBlocks = htmlBlocks.flatten.mkString("\n")
    Seq(combinedBlocks)
  }

  private def sectionFrom(tag: Tag): Section =
    Section(tag.id, tag.webTitle, AbsoluteURL.from(tag.webUrl))

  private def addSpacesAround(html: String) = s"<p>&nbsp;</p>$html<p>&nbsp;</p>"

  private def emphasiseTweet(html: String): String = {
    val doc = Jsoup.parseBodyFragment(html)
    doc.select("blockquote p").forEach { p => p.html(s"<em>${p.html}</em>") }
    doc.body.html
  }

  private val cartoonTagIds: Set[String] = settings.cartoonTags.map(_.id)

}
