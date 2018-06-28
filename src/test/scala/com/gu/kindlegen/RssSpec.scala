package com.gu.kindlegen

import java.time.{Instant, LocalDate, ZoneOffset}
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
import java.time.temporal.ChronoUnit.SECONDS

import org.scalatest.FunSpec
import org.scalatest.Inspectors._
import org.scalatest.Matchers._

import com.gu.kindlegen.TestData._


class RssSpec extends FunSpec {
  val title = "A <title> &entity;"  // shouldn't be escaped until it's actually serialised

  describe("RssItem") {
    val item = RssItem(title, ExampleLink)
    def textOf(field: String) = (item.rss \ field).text

    it("generates a title") { textOf("title") shouldBe title }
    it("generates a link")  { textOf("link" ) shouldBe ExampleLink.source }
  }

  describe("RssManifest") {
    val now = Instant.now
    val today = now.atOffset(ZoneOffset.UTC).toLocalDate
    val items = (0 until 3).map(_.toString).map(RssItem(_, ExampleLink))
    val manifest = RssManifest(title, ExampleLink, items, today, now)

    def nodesOf(field: String) = manifest.rss \ "channel" \ field
    def textOf(field: String) = nodesOf(field).text

    it("generates a title") { textOf("title") shouldBe title }
    it("generates a link" ) { textOf("link" ) shouldBe ExampleLink.source }

    it("generates a publication date in the correct format") {
      LocalDate.parse(textOf("pubDate")) shouldBe today
    }

    it("generates a last build date in the correct format") {
      Instant.from(RFC_1123_DATE_TIME.parse(textOf("lastBuildDate"))) shouldBe now.truncatedTo(SECONDS)
    }

    it("generates items") {
      val itemNodes = nodesOf("item")
      itemNodes should have length items.length

      forAll(itemNodes.zipWithIndex) { case (item, index) =>
        (item \ "title").text shouldBe index.toString
      }
    }
  }

}
