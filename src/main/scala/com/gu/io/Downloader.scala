package com.gu.io

import java.nio.file.Path

import scala.concurrent.Future

trait Downloader {
  def download(url: String): Future[Array[Byte]]
  def downloadAs(path: Path, url: String): Future[Path]
}
