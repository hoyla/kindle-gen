package com.gu.kindlegen

import com.gu.contentapi.client.model.v1.{Asset, Content, Element, ElementType}


case class Image(id: String,
                 url: String,
                 altText: Option[String],
                 caption: Option[String],
                 credit: Option[String])

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
    } yield
      Image(
        id = mainImage.id,
        url = url,
        altText = metadata.altText,
        caption = metadata.caption,
        credit = if (metadata.displayCredit.getOrElse(true)) metadata.credit else None
      )
  }
}
