package com.gu.kindlegen

import com.gu.contentapi.client.model.v1.{Asset, Content, Element, ElementType}
import com.gu.io.{Link, Linkable}


case class Image(id: String,
                 link: Link,
                 altText: Option[String],
                 caption: Option[String],
                 credit: Option[String]) extends Linkable

object Image {
  def mainImage(content: Content, settings: QuerySettings): Option[Image] = {
    def isMainImage(e: Element): Boolean =
      e.`type` == ElementType.Image && e.relation == "main"

    def bestAcceptable(assets: Seq[Asset]): Option[Asset] = {
      assets.iterator.flatMap(MeasurableAsset.apply)
        .filter(_.maxDimension <= settings.maxImageResolution)
        .reduceOption(MeasurableAsset.ordering.max)
        .map(_.asset)
    }

    for {
      elements <- content.elements
      mainImage <- elements.find(isMainImage)
      bestAsset <- bestAcceptable(mainImage.assets)
      metadata <- bestAsset.typeData
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

  private class MeasurableAsset(val asset: Asset, val maxDimension: Int)
  private object MeasurableAsset {
    def apply(asset: Asset): Option[MeasurableAsset] =
      asset.typeData
        .flatMap { fields => Seq(fields.height, fields.width).max }
        .map(new MeasurableAsset(asset, _))

    def ordering: Ordering[MeasurableAsset] = Ordering.by(_.maxDimension)
  }
}
