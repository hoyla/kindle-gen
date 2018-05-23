package com.gu.kindlegen

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

import org.apache.logging.log4j.scala.Logging

import com.gu.io.IOUtils


case class ImageData(metadata: Image, data: Array[Byte]) {
  def source: String = metadata.link.source
}

object ImageData extends Logging {
  def download(image: Image)(implicit ec: ExecutionContext): Future[ImageData] = {
    val link = image.link
    logger.info(s"Downloading image from $link")

    IOUtils.download(link.source)
      .map(bytes => ImageData(image, bytes))
      .andThen {
        case Failure(error) => logger.error(s"Failed to download image from $link!", error)
      }
  }
}
