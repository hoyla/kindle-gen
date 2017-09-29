package com.gu.kindlegen

import com.gu.contentapi.client.model.v1.CapiDateTime
import org.joda.time.DateTime
import org.joda.time.format.{ DateTimeFormat, ISODateTimeFormat }

class DateUtils {
}

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
}
