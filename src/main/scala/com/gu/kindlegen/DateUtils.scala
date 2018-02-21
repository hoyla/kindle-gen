package com.gu.kindlegen

import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoUnit, TemporalAccessor}
import java.time.{Instant, LocalDate, ZoneId}

import com.gu.contentapi.client.model.v1.CapiDateTime
import com.gu.contentapi.client.utils.CapiModelEnrichment.RichCapiDateTime

object DateUtils {
  // formatter for use with print to convert a DateTime: Long to DateTime String
  def formatter = DateTimeFormat.forPattern("yyyyMMdd")
  def dtFormatter = DateTimeFormat.forPattern("yyyyMMddHHmmss")

  // formatter for use with parseDateTime to convert an isoDateTime: String to isoDateTime: DateTime
  private def isoFormatter = DateTimeFormat.isoDateTime()
  private def formatterWithDashes = DateTimeFormat.forPattern("yyyy-MM-dd")

  def formatDate(capiDate: CapiDateTime): String = formatter.format(capiDate.toOffsetDateTime)

  // TODO: change this to DateTime.now or function that takes a passed in date.
  private def editionDateTime: Instant = Instant.parse("2017-05-19T00:00:00Z") // to have a date I know the results for
  //  def editionDateTime: DateTime = DateTime.now()

  def editionDateStart: Instant = editionDateTime
  def editionDateEnd: Instant = editionDateTime.plus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.NANOS)

  /* Adaptor classes to simplify the migration from Joda-Time to Java 8 Time */

  object DateTime {
    def now() =
      new DateTime(Instant.now)
  }

  final class DateTime(instant: Instant) {
    def this(temporalAccessor: TemporalAccessor) =
      this(Instant.from(temporalAccessor))

    def toCapiDateTime: CapiDateTime =
      CapiDateTime.apply(instant.toEpochMilli, instant.toString)
  }

  object DateTimeFormat {
    def forPattern(pattern: String) =
      new DateTimeFormatter(DateTimeFormatter.ofPattern(pattern))

    def isoDateTime() =
      new DateTimeFormatter(DateTimeFormatter.ISO_DATE_TIME)
  }

  final class DateTimeFormatter(formatter: java.time.format.DateTimeFormatter) {
    def parseDate(text: String): DateTime = {
      val parsed = formatter.parse(text)
      new DateTime(LocalDate.from(parsed).atStartOfDay(ZoneId.of("UTC")))
    }

    def format(dateTime: DateTime): String =
      formatter.format(dateTime.toCapiDateTime.toOffsetDateTime)

    def format(temporal: TemporalAccessor) = formatter.format(temporal)
  }
}
