package com.gu.kindlegen

import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

import com.gu.contentapi.client.model.v1.CapiDateTime
import com.gu.contentapi.client.utils.CapiModelEnrichment._

object DateUtils {
  private def formatter = DateTimeFormatter.ofPattern("yyyyMMdd")

  val dtFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

  // formatter for use with parseDateTime to convert an isoDateTime: String to isoDateTime: DateTime
  def formatDate(capiDate: CapiDateTime): String = formatter.format(capiDate.toOffsetDateTime)

  val exampleDate = OffsetDateTime.of(2017, 7, 24, 0, 0, 0, 0, ZoneOffset.UTC).toCapiDateTime
}
