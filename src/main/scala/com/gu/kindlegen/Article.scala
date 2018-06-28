package com.gu.kindlegen

import java.time.OffsetDateTime

import com.gu.io.{Link, Linkable}


case class Article(
    section: Section,
    newspaperPageNumber: Int,
    title: String,
    docId: String,
    link: Link,
    pubDate: OffsetDateTime,
    byline: String,
    articleAbstract: String,
    bodyBlocks: Seq[String],
    mainImage: Option[Image]) extends Linkable
