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
  // TODO: get rid of the gets
  def apply(contentWithIndex: (Content, Int)): Article = {
    val content = contentWithIndex._1
    val index = contentWithIndex._2
    //    for {
    //      newspaperBookSection <- content.tags.find(_.`type` == NewspaperBookSection)
    //    } Article(
    Article(
      newspaperBookSection = content.tags.find(_.`type` == NewspaperBookSection).get.id,
      sectionName = content.tags.find(_.`type` == NewspaperBookSection).get.webTitle,
      newspaperPageNumber = content.fields.flatMap(_.newspaperPageNumber).getOrElse(0),
      title = content.fields.flatMap(_.headline).getOrElse("").toString,
      docId = content.id,
      issueDate = content.fields.flatMap(_.newspaperEditionDate).get,
      releaseDate = content.fields.flatMap(_.newspaperEditionDate).get,
      pubDate = content.fields.flatMap(_.newspaperEditionDate).get,
      byline = content.fields.flatMap(_.byline).getOrElse(""), articleAbstract = content.fields.flatMap(_.standfirst).getOrElse(""),
      content = content.fields.flatMap(_.body).getOrElse(""),
      imageUrl = getImageUrl(content: Content),
      fileId = index
    )
  }
}