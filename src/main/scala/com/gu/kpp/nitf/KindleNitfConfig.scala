package com.gu.kpp.nitf

import com.gu.nitf.NitfConfig


/** Information about NITF extensions in Kindle Publishing for Periodicals (KPP) */
// see src/main/resources/kpp-nitf-3.5.7.xsd
object KindleNitfConfig extends NitfConfig {
  /** KPP tags that may be contained within [[enrichedTextParentTags]] */
  val extraEnrichedTextOnlyParentTags: Set[String] = Set("h3", "h4", "h5", "h6", "img")

  /** KPP tags whose contents may only be one of [[enrichedTextTags]]. */
  val extraEnrichedTextParentTags: Set[String] = extraEnrichedTextOnlyParentTags

  /** All custom KPP tags */
  val extraTags: Set[String] = extraEnrichedTextParentTags ++ Set("content", "strong")

  override val allTags: Set[String] = NitfConfig.allTags ++ extraTags
  override val enrichedTextParentTags: Set[String] = NitfConfig.enrichedTextParentTags ++ extraEnrichedTextParentTags
  override val enrichedTextOnlyParentTags: Set[String] = NitfConfig.enrichedTextOnlyParentTags ++ extraEnrichedTextOnlyParentTags

  override val enrichedTextTags: Set[String] = NitfConfig.enrichedTextTags
  override val nonEmptyTags: Set[String] = NitfConfig.nonEmptyTags
}
