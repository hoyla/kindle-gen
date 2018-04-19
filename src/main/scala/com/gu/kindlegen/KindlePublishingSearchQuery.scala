package com.gu.kindlegen

import java.time.{LocalDate, ZoneOffset}
import java.time.temporal.ChronoUnit.DAYS

import com.gu.contentapi.client.model.PrintSentSearchQuery

object KindlePublishingSearchQuery {
  // pagination is not necessary nor desired for a print-sent query
  // it only makes it slower and there are only ~200 per day
  // additionally, `newspaper-edition` date is always set to midnight, resulting in duplicate results across pages
  val AllResultsPageSize = 400  // maximum page size accepted by CAPI

  val WhiteListedTags = Seq.empty[String]  // leave empty to get all tags
  val BlackListedTags = Seq("type/interactive")

  val ResponseTags = Seq("newspaper-book", "type")
  val ResponseFields = Seq("byline", "newspaper-edition-date", "newspaper-page-number", "standfirst")

  def apply(date: LocalDate,
            publishingZone: ZoneOffset = ZoneOffset.UTC,
            whiteListedTags: Seq[String] = WhiteListedTags,
            blackListedTags: Seq[String] = BlackListedTags): PrintSentSearchQuery = {
    val startOfDay = date.atStartOfDay.toInstant(publishingZone)
    val endOfDay = startOfDay.plus(1, DAYS).minusNanos(1)

    PrintSentSearchQuery()
      .tag(combineParams(WhiteListedTags ++ negate(BlackListedTags)))
      .fromDate(startOfDay)
      .toDate(endOfDay)
      .useDate("newspaper-edition")
      .showBlocks("body")
      .showElements("image")
      .showTags(combineParams(ResponseTags))
      .showFields(combineParams(ResponseFields))
      .pageSize(AllResultsPageSize)
  }

  @inline private def combineParams(strings: Seq[String]) = strings.mkString(",")
  @inline private def negate(strings: Seq[String]) = strings.map("-" + _)
}
