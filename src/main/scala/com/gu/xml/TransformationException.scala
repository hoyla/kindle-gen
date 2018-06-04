package com.gu.xml


class TransformationException(val message: Option[String], val cause: Option[Throwable])
    extends RuntimeException(message.orNull, cause.orNull) {

  def this(message: String) = this(Option(message), None)
  def this(message: String, cause: Throwable) = this(Option(message), Option(cause))
}
