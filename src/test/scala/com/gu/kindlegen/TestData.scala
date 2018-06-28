package com.gu.kindlegen

import java.time.{OffsetDateTime, ZoneOffset}

import com.gu.io.Link

object TestData {
  val ExampleOffsetDate: OffsetDateTime = OffsetDateTime.of(2017, 7, 24, 0, 0, 0, 0, ZoneOffset.UTC)
  val ExampleLink = Link.AbsoluteURL.from("https://www.example.com")
}
