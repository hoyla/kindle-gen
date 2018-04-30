package com.gu.kindlegen

import java.time._
import java.time.format.DateTimeFormatter

import com.gu.contentapi.client.model.v1.CapiDateTime
import com.gu.contentapi.client.utils.CapiModelEnrichment._

object DateUtils {
  private def formatter = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC"))

  val dtFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("UTC"))

  // formatter for use with parseDateTime to convert an isoDateTime: String to isoDateTime: DateTime
  def formatDate(capiDate: CapiDateTime): String = formatter.format(capiDate.toOffsetDateTime)
}
