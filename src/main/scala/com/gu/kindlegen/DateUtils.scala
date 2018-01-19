package com.gu.kindlegen

import com.gu.contentapi.client.model.v1.CapiDateTime

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

  protected[DateUtils] trait ReadableInstant { def toInstant: org.joda.time.ReadableInstant }
  implicit def readableInstantAdaptor2ReadableInstant(readableInstantAdaptor: ReadableInstant): org.joda.time.ReadableInstant =
    readableInstantAdaptor.toInstant

  object CapiModelEnrichment {
    implicit final class RichJodaDateTime(val readableInstantAdaptor: ReadableInstant) extends AnyVal {
      private def dt = readableInstantAdaptor.toInstant
      def toCapiDateTime: CapiDateTime = CapiDateTime.apply(dt.getMillis, dt.toString)
    }
  }

  object DateTime {
    def parse(pattern: String) =
      new DateTime(org.joda.time.DateTime.parse(pattern))
    def now() =
      new DateTime(org.joda.time.DateTime.now)
  }

  final class DateTime(dateTime: org.joda.time.DateTime) extends ReadableInstant {
    override def toInstant: org.joda.time.ReadableInstant = dateTime
    def withHourOfDay(millis: Int) = new DateTime(dateTime.withHourOfDay(millis))
    def withMinuteOfHour(millis: Int) = new DateTime(dateTime.withMinuteOfHour(millis))
    def withSecondOfMinute(millis: Int) = new DateTime(dateTime.withSecondOfMinute(millis))
    def withMillisOfSecond(millis: Int) = new DateTime(dateTime.withMillisOfSecond(millis))
    def withMillisOfDay(millis: Int) = new DateTime(dateTime.withMillisOfDay(millis))
  }

  object DateTimeFormat {
    def forPattern(pattern: String) =
      new DateTimeFormatter(org.joda.time.format.DateTimeFormat.forPattern(pattern))
  }

  final class DateTimeFormatter(formatter: org.joda.time.format.DateTimeFormatter) {
    def parseDateTime(text: String): DateTime =
      new DateTime(formatter.parseDateTime(text))

    def print(instant: ReadableInstant): String =
      formatter.print(instant.toInstant)
  }

  object ISODateTimeFormat {
    def dateTime() =
      new DateTimeFormatter(org.joda.time.format.ISODateTimeFormat.dateTime())
  }
}
