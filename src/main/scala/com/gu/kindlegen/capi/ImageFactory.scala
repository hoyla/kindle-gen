package com.gu.kindlegen.capi

import com.gu.contentapi.client.model.v1.{Asset, Content, Element, ElementType}
import com.gu.io.Link
import com.gu.kindlegen.Image


class ImageFactory(settings: GuardianProviderSettings) {
  def mainImage(content: Content,
                captionFallback: Option[String] = None): Option[Image] = {
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
    } yield {
      val caption = (metadata.caption ++ captionFallback).map(_.trim).find(_.nonEmpty)
      Image(
        id = mainImage.id,
        link = absoluteUrl,
        altText = metadata.altText,
        caption = caption,
        credit = if (metadata.displayCredit.getOrElse(true)) metadata.credit else None
      )
    }
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
