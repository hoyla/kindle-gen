package com.gu.kindlegen

import scala.collection.breakOut

import better.files._


object Resources {
  val NitfSchemaPath = "kpp-nitf-3.5.7.xsd"
  val XmlSchemaPath = "xml.xsd"

  // an ordered list of schema in topological order (schema references earlier ones)
  lazy val NitfSchemasContents: Seq[String] = {
    implicit val c = DefaultCharset
    (Resource.asString(XmlSchemaPath) zip Resource.asString(NitfSchemaPath)).flatMap {
      case (x, y) => Seq(x, y)
    }(breakOut)
  }
}
