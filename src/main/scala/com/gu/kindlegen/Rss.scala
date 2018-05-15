package com.gu.kindlegen

import java.time.{Instant, LocalDate, ZoneOffset}
import java.time.format.DateTimeFormatter

import scala.xml.Elem

import com.gu.io.{Link, Linkable}


case class RssItem(title: String, link: Link) extends Linkable {
  def rss: Elem =
    <item>
      <title>{title}</title>
      <link>{link.source}</link>
    </item>
}

case class RssManifest(title: String,
                       link: Link,
                       items: Seq[RssItem],
                       publicationDate: LocalDate,
                       buildInstant: Instant) extends Linkable {
  import RssManifest._

  def asRssItem = RssItem(title, link)

  def rss: Elem =
    <rss version="2.0">
      <channel>
        <title>{title}</title>
        <link>{link.source}</link>
        <pubDate>{publicationDate}</pubDate>
        <lastBuildDate>{formatter.format(buildInstant)}</lastBuildDate>
        {items.map(_.rss)}
      </channel>
    </rss>
}

object RssManifest {
  private final val Encoding = "UTF-8"

  private val formatter = DateTimeFormatter
    .ofPattern("EEE, dd MMM yyyy HH:mm:ss Z")  // Tue, 01 May 2018 17:10:54 +0000
    .withZone(ZoneOffset.UTC)
}
