package com.gu.kindlegen

import com.gu.contentapi.client.model.v1.{Asset, Content, Element, ElementType}


case class Image(id: String,
                 link: Link,
                 altText: Option[String],
                 caption: Option[String],
                 credit: Option[String]) extends Linkable

object Image {
  def mainMaster(content: Content): Option[Image] = {
    def isMainImage(e: Element): Boolean =
      e.`type` == ElementType.Image && e.relation == "main"

    def isMasterAsset(a: Asset): Boolean =
      a.typeData.flatMap(_.isMaster).getOrElse(false)

    for {
      elements <- content.elements
      mainImage <- elements.find(isMainImage)
      masterAsset <- mainImage.assets.find(isMasterAsset)
      metadata <- masterAsset.typeData
      url <- metadata.secureFile
      absoluteUrl <- Link.AbsoluteURL(url).toOption
    } yield
      Image(
        id = mainImage.id,
        link = absoluteUrl,
        altText = metadata.altText,
        caption = metadata.caption,
        credit = if (metadata.displayCredit.getOrElse(true)) metadata.credit else None
      )
  }
}
