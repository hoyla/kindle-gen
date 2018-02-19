package com.gu.kindlegen

import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist

/**
 * Filters to be applied to body content - preserve certain html tags. Strips anchor tags, apply other necessary formatting for Amazon kindle NITF format
 */
// TODO: Check whether tweet embedded text is stripped or kept. (Should be kept)
object FilterUtil {
  val whitelist =
    new Whitelist()
      .addTags("p")
      .addTags("b", "em", "i", "strong", "u")
      .addTags("br")
      .addTags("blockquote", "cite", "q", "footer")
      .addAttributes("q", "cite")

  def apply(input: String): String = {
    val convertedInput = input.replaceAll("""’""", "'") // convert apostrophe
    Jsoup.clean(convertedInput, whitelist)
  }
}
