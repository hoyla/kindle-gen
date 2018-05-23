package com.gu.kpp.nitf

import com.gu.nitf.NitfConfig


object KindleNitfConfig extends KindleNitfConfig

/** Information about NITF extensions in Kindle Publishing for Periodicals */
// see src/main/resources/kpp-nitf-3.5.7.xsd
trait KindleNitfConfig extends NitfConfig {
  val extraBlockContentTags = Set("content", "h3", "h4", "h5", "h6", "img")
  val extraTags: Set[String] = extraBlockContentTags ++ Set("strong")

  override val allTags: Set[String] = NitfConfig.allTags ++ extraTags

  override val blockContentTags: Set[String] = NitfConfig.blockContentTags ++ extraBlockContentTags
}
