package com.gu.contentapi.client

abstract class AbstractContentApiClient(override val apiKey: String, customTargetUrl: Option[String] = None)
    extends ContentApiClient {
  override val targetUrl: String = customTargetUrl.getOrElse(super.targetUrl)
}
