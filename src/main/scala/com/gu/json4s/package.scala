package com.gu.json4s

import scala.reflect.{classTag, ClassTag}
import scala.language.implicitConversions

import org.json4s.{JValue, JNothing}


object `package` {
  implicit def jValueToRichOptionalJValue(obj: JValue): RichOptionalJValue = new RichOptionalJValue(Some(obj))

  implicit final class RichOptionalJValue(val obj: Option[JValue]) extends AnyVal {
    /** Attempt to extract a defined value, i.e. one that is neither JNull nor JNothing
      * Example usage: json.detect(_ \ "my" \ "property")
      */
    def detect(extractor: JValue => JValue): Option[JValue] =
      obj.flatMap(extractor(_).toOption)

    /** Attempt to extract a defined value, i.e. one that is neither JNull nor JNothing.
      * Example usage: json.detect(_ \ "my" \ "array").detectOpt(_.find(elementMatcher))
      */
    def detectOpt(extractor: JValue => Option[JValue]): Option[JValue] =
      obj.flatMap(extractor).flatMap(_.toOption)

    /** Attempt to extract the Scala-equivalent of a defined value.
      * Example usage: val str: String = json.detectValue[JString](_ \ "my" \ "property")
      *
      * Note that if the value doesn't match the expected type then the result will be None.
      * This manifests itself, for example, when you're expecting a JDouble but the JSON value looks like a JInt.
      */
    def detectValue[T <: JValue: ClassTag](extractor: JValue => JValue): Option[T#Values] =
      detect(extractor).toIterable.flatMap(_ \\ runtimeClass[T]).headOption

    @inline
    private def runtimeClass[T: ClassTag]: Class[T] =
      classTag[T].runtimeClass.asInstanceOf[Class[T]]  // the cast is safe because all descendants of JValue have no type parameters
  }
}
