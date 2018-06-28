package com.gu.kindlegen

import com.gu.io.{Link, Linkable}


case class Image(id: String,
                 link: Link,
                 altText: Option[String],
                 caption: Option[String],
                 credit: Option[String]) extends Linkable

case class ImageData(metadata: Image, data: Array[Byte]) {
  def source: String = metadata.link.source
}
