package com.gu.kpp.nitf

import com.gu.nitf.HtmlToNitfConfig


object KindleHtmlToNitfConfig extends HtmlToNitfConfig {
  override def nitf: KindleNitfConfig.type = KindleNitfConfig

  override val blacklist: Set[String] = HtmlToNitfConfig.blacklist -- KindleNitfConfig.extraTags

  override val equivalentNitfTag: Map[String, String] = HtmlToNitfConfig.equivalentNitfTag ++ Map(
    "b"   -> "strong",
    "big" -> "strong",
  )

  override def supportedNitfTags: Set[String] = HtmlToNitfConfig.supportedNitfTags ++ nitf.extraTags
}
