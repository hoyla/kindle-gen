package com.gu.io.sttp

import scala.concurrent.ExecutionContext

import com.softwaremill.sttp.okhttp.OkHttpFutureBackend


class OkHttpSttpDownloader(implicit protected override val ec: ExecutionContext) extends {
  protected override val backend = OkHttpFutureBackend()
} with SttpDownloader {
  protected type StreamingCapability = Nothing
}

object OkHttpSttpDownloader {
  def apply(implicit ec: ExecutionContext = ExecutionContext.Implicits.global) = new OkHttpSttpDownloader()
}
