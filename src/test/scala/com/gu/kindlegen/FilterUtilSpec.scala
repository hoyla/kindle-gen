package com.gu.kindlegen

import org.scalatest.FlatSpec
import FilterUtil._

class FilterUtilSpec extends FlatSpec {

  "FilterUtil" should "strip a href links leaving inner html" in {
    val bodyFromApi = """<p>On paper, there’s quite literally nothing like Colossal. Writer/director Nacho Vigalondo deserves immediate plaudits for crafting a premise that reads like Rachel Getting Married meets Godzilla (in fact, the film’s resemblance to the latter <a href="https://www.theguardian.com/film/2015/may/20/godzilla-toho-sues-over-anne-hathaway-rival-movie-colossal">led to a lawsuit</a>) and the leftfield genre shift comes as a refreshing change, after another particularly dull set of summer blockbusters. But, given the bizarro conceit, there’s something surprisingly, and frustratingly, safe about the film.</p>"""

    val expectedXML =
      """<p>On paper, there's quite literally nothing like Colossal. Writer/director Nacho Vigalondo deserves immediate plaudits for crafting a premise that reads like Rachel Getting Married meets Godzilla (in fact, the film's resemblance to the latter led to a lawsuit) and the leftfield genre shift comes as a refreshing change, after another particularly dull set of summer blockbusters. But, given the bizarro conceit, there's something surprisingly, and frustratingly, safe about the film.</p>"""
    assert(FilterUtil(bodyFromApi) === expectedXML)
  }

  it should "convert apostrophes" in {
    val bodyContent = """<p>To call Colossal tonally uneven would perhaps be missing the entire point of Colossal. For months now, the staggeringly odd premise has been the source of feverish online discussion and intense confusion. She did what? And has a what? But how could that? The answers are here and, well, they’re far from befitting of that title ...</p>"""

    val expectedFilteredBodyContent =
      """<p>To call Colossal tonally uneven would perhaps be missing the entire point of Colossal. For months now, the staggeringly odd premise has been the source of feverish online discussion and intense confusion. She did what? And has a what? But how could that? The answers are here and, well, they're far from befitting of that title ...</p>"""

    assert(FilterUtil(bodyContent) === expectedFilteredBodyContent)
  }
}
