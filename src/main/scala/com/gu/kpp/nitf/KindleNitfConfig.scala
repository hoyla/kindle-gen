package com.gu.kpp.nitf

import com.gu.nitf.NitfConfig


/** Information about NITF extensions in Kindle Publishing for Periodicals */
// see src/main/resources/kpp-nitf-3.5.7.xsd
object KindleNitfConfig extends NitfConfig {
  val extraEnrichedTextOnlyParentTags: Set[String] = Set("h3", "h4", "h5", "h6", "img")
  val extraEnrichedTextParentTags: Set[String] = extraEnrichedTextOnlyParentTags
  val extraTags: Set[String] = extraEnrichedTextParentTags ++ Set("content", "strong")

  val allTags: Set[String] = NitfConfig.allTags ++ extraTags
  val enrichedTextParentTags: Set[String] = NitfConfig.enrichedTextParentTags ++ extraEnrichedTextParentTags
  val enrichedTextOnlyParentTags: Set[String] = NitfConfig.enrichedTextOnlyParentTags ++ extraEnrichedTextOnlyParentTags

  val enrichedTextTags: Set[String] = NitfConfig.enrichedTextTags
  val nonEmptyTags: Set[String] = NitfConfig.nonEmptyTags
}
