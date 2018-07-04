package com.gu.kindlegen

import java.nio.file.{Files, Path}

import com.gu.io.IOUtils

object Resources {
  lazy val NitfSchemaPath: Option[Path] = IOUtils.resourceAsPath("kpp-nitf-3.5.7.xsd")
  lazy val XmlSchemaPath: Option[Path] = IOUtils.resourceAsPath("xml.xsd")

  // an ordered list of schema in topological order (schema references earlier ones)
  lazy val XmlSchemaContents: Seq[Array[Byte]] = XmlSchemaPath.map(Files.readAllBytes).toSeq
  lazy val NitfSchemaContents: Seq[Array[Byte]] = XmlSchemaContents ++ NitfSchemaPath.map(Files.readAllBytes)
}
