package com.gu.kindlegen.capi

import java.time.{LocalDate, ZoneOffset}

import com.gu.contentapi.client.model.PrintSentSearchQuery
import com.gu.contentapi.client.model.v1.{ElementType, TagType}

object KindlePublishingSearchQuery {
  // pagination is neither necessary nor desired for a print-sent query
  // it only makes it slower and there are only ~200 per day
  // additionally, `newspaper-edition` date is always set to midnight, resulting in duplicate results across pages
  val MaxResultsPageSize = 400  // maximum page size accepted by CAPI

  val WhiteListedTags = Seq.empty[String]  // leave empty to get all tags
  val BlackListedTags = Seq("type/interactive")


  def apply(date: LocalDate,
            showBlocks: Set[String],
            showElements: Set[ElementType],
            showFields: Set[String],
            showTagTypes: Set[TagType],
            whiteListedTags: Seq[String] = WhiteListedTags,
            blackListedTags: Seq[String] = BlackListedTags): PrintSentSearchQuery = {

    val newspaperEditionDate = date.atStartOfDay.toInstant(ZoneOffset.UTC)

    PrintSentSearchQuery()
      .tag(combineParams(WhiteListedTags ++ negate(BlackListedTags)))
      .fromDate(newspaperEditionDate)
      .toDate(newspaperEditionDate)
      .useDate("newspaper-edition")
      .showBlocks(combineParams(showBlocks.map(_.toLowerCase)))
      .showElements(combineParams(showElements.map(_.name.toLowerCase)))
      .showTags(combineParams(showTagTypes.map(_.id)))
      .showFields(combineParams(showFields))
      .pageSize(MaxResultsPageSize)
  }

  @inline private def combineParams(strings: Iterable[String]) = strings.mkString(",")
  @inline private def negate(strings: Iterable[String]) = strings.map("-" + _)
}
