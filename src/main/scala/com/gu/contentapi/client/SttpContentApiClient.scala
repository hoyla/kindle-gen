package com.gu.contentapi.client

import scala.concurrent.{ExecutionContext, Future}

import com.softwaremill.sttp._

import com.gu.contentapi.client.model.HttpResponse
import com.gu.io.sttp.SttpDownloader


trait SttpContentApiClient extends ContentApiClient {
  protected def downloader: SttpDownloader

  override def get(url: String, headers: Map[String, String])(implicit context: ExecutionContext): Future[HttpResponse] = {
    val request = sttp.get(uri"$url").headers(headers).response(asByteArray)

    downloader.send(request).map { response =>
      HttpResponse(response.unsafeBody, response.code, response.statusText)
    }
  }
}
