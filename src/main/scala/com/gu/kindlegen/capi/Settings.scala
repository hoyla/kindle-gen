package com.gu.kindlegen.capi

import scala.concurrent.duration.FiniteDuration

import com.gu.contentapi.client.model.v1.TagType


final case class ContentApiCredentials(key: String, url: String)

final case class GuardianProviderSettings(downloadTimeout: FiniteDuration,
                                          sectionTagType: TagType,
                                          maxImageResolution: Int)
