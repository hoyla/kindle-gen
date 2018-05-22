package com.gu.kpp.nitf

import com.gu.nitf.NitfConfig


object KindleNitfConfig extends KindleNitfConfig

trait KindleNitfConfig extends NitfConfig {
  val extraBlockContentTags = Set("content", "h3", "h4", "h5", "h6", "img")
  val extraTags: Set[String] = extraBlockContentTags ++ Set("strong")

  override val tags: Set[String] = NitfConfig.tags ++ extraTags

  override val blockContentTags: Set[String] = NitfConfig.blockContentTags ++ extraBlockContentTags
}
