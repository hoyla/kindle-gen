package com.gu.kindlegen

import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoField, TemporalAccessor}
import java.time.{Instant, LocalDate, ZoneId, ZonedDateTime}

import com.gu.contentapi.client.model.v1.CapiDateTime

import scala.util.Try

object DateUtils {
  // formatter for use with print to convert a DateTime: Long to DateTime String
  def formatter = DateTimeFormat.forPattern("yyyyMMdd")
  def dtFormatter = DateTimeFormat.forPattern("yyyyMMddHHmmss")

  // formatter for use with parseDateTime to convert an isoDateTime: String to isoDateTime: DateTime
  def isoFormatter = ISODateTimeFormat.dateTime()
  def formatterWithDashes = DateTimeFormat.forPattern("yyyy-MM-dd")

  def capiIsoDateTimeToString(capidatetime: CapiDateTime): String = capidatetime.iso8601

  def isoDateConverter(capiDate: CapiDateTime): String = formatter.print(isoFormatter.parseDateTime(capiIsoDateTimeToString(capiDate)))

  // TODO: change this to DateTime.now or function that takes a passed in date.
  // TODO: should be Z format not +XXXX hours?
  def editionDateTime: DateTime = formatterWithDashes.parseDateTime("2017-05-19") // to have a date I know the results for
  //  def editionDateTime: DateTime = DateTime.now()

  def editionDateString: String = formatterWithDashes.print(editionDateTime)
  def editionDateStart: DateTime = DateTime.parse(editionDateString).withMillisOfDay(0).withMillisOfSecond(0)
  def editionDateEnd: DateTime = DateTime.parse(editionDateString).withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59).withMillisOfSecond(999)

  /* Temporary adaptor classes in preparation to replace Joda Time with Java 8 Time */

  protected[DateUtils] trait ReadableInstant { def toInstant: Instant }
  implicit def readableInstantAdaptor2ReadableInstant(readableInstantAdaptor: ReadableInstant): Instant =
    readableInstantAdaptor.toInstant

  object CapiModelEnrichment {
    implicit final class RichJodaDateTime(val readableInstantAdaptor: ReadableInstant) extends AnyVal {
      private def dt = readableInstantAdaptor.toInstant
      def toCapiDateTime: CapiDateTime = CapiDateTime.apply(dt.toEpochMilli, dt.toString)
    }
  }

  object DateTime {
    def parse(pattern: String) =
      new DateTime(DateTimeFormatter.ISO_DATE_TIME.parse(pattern))
    def now() =
      new DateTime(Instant.now)
  }

  final class DateTime(protected[DateUtils] val instant: Instant) extends ReadableInstant {
    def this(temporalAccessor: TemporalAccessor) =
      this(Instant.from(temporalAccessor))

    override def toInstant: Instant = instant

    import ChronoField._
    def withHourOfDay(hour: Int): DateTime = withField(HOUR_OF_DAY, hour)
    def withMinuteOfHour(minute: Int): DateTime = withField(MINUTE_OF_HOUR, minute)
    def withSecondOfMinute(second: Int): DateTime = withField(SECOND_OF_MINUTE, second)
    def withMillisOfSecond(millis: Int): DateTime = withField(MILLI_OF_SECOND, millis)
    def withMillisOfDay(millis: Int): DateTime = withField(MILLI_OF_DAY, millis)

    private def withField(field: ChronoField, value: Int): DateTime =
      new DateTime(instant.`with`(field, value))
  }

  object DateTimeFormat {
    def forPattern(pattern: String) =
      new DateTimeFormatter(DateTimeFormatter.ofPattern(pattern))
  }

  final class DateTimeFormatter(formatter: java.time.format.DateTimeFormatter) {
    def parseDateTime(text: String): DateTime = {
      val parsed = formatter.parse(text)
      new DateTime(Try(Instant.from(parsed)).getOrElse(LocalDate.from(parsed).atStartOfDay(ZoneId.systemDefault)))
    }

    def print(dateTime: DateTime): String =
      formatter.format(ZonedDateTime.ofInstant(dateTime.instant, ZoneId.systemDefault()))
  }

  object ISODateTimeFormat {
    def dateTime() =
      new DateTimeFormatter(DateTimeFormatter.ISO_DATE_TIME)
  }
}
