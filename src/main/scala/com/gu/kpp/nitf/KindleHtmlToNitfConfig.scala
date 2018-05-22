package com.gu.kpp.nitf

import com.gu.nitf.HtmlToNitfConfig


object KindleHtmlToNitfConfig extends HtmlToNitfConfig {
  def nitf: KindleNitfConfig = KindleNitfConfig

  override val blacklist: Set[String] = HtmlToNitfConfig.blacklist -- KindleNitfConfig.extraTags

  override val equivalentNitfTag: Map[String, String] = HtmlToNitfConfig.equivalentNitfTag ++ Map(
    "b"   -> "strong",
    "big" -> "strong",
  )
}
