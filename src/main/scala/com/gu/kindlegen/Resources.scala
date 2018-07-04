package com.gu.kindlegen

import scala.collection.breakOut

import com.gu.io.IOUtils.resourceAsString

object Resources {
  val NitfSchemaPath = "kpp-nitf-3.5.7.xsd"
  val XmlSchemaPath = "xml.xsd"

  // an ordered list of schema in topological order (schema references earlier ones)
  lazy val NitfSchemasContents: Seq[String] = {
    (resourceAsString(XmlSchemaPath) zip resourceAsString(NitfSchemaPath)).flatMap {
      case (x, y) => Seq(x, y)
    }(breakOut)
  }
}
