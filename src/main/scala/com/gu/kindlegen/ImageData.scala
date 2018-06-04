package com.gu.kindlegen

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

import org.apache.logging.log4j.scala.Logging

import com.gu.io.Downloader


case class ImageData(metadata: Image, data: Array[Byte]) {
  def source: String = metadata.link.source
}

object ImageData extends Logging {
  def download(image: Image, downloader: Downloader)(implicit ec: ExecutionContext): Future[ImageData] = {
    val link = image.link
    logger.info(s"Downloading image from $link")

    downloader.download(link.source)
      .map(bytes => ImageData(image, bytes))
      .andThen {
        case Failure(error) => logger.error(s"Failed to download image from $link!", error)
      }
  }
}
