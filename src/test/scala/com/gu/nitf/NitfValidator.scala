package com.gu.nitf

import java.io.File
import java.nio.file.{Files, Paths}

import scala.collection.JavaConverters._
import scala.xml._

import org.scalatest.FunSpec

import com.gu.kpp.nitf.XhtmlToNitfTransformer
import com.gu.xml._
import com.gu.xml.XmlUtils._

class NitfValidator extends FunSpec {
  import NitfValidator._

  Seq(Paths.get(resource("xhtml-example.nitf").toURI))
  .foreach { nitfFilePath =>
      describe("NITF file " + nitfFilePath) {
        it("should match the schema") {
          try {
            val xml = loadAndTransform(nitfFilePath.toFile)
            validateXml(xml, "kpp-nitf-3.5.7.xsd")
          } catch {
            case e: org.xml.sax.SAXParseException =>
              cancel("XML file is invalid! " + e.getMessage, e)
          }
        }
      }
    }

  private def loadAndTransform(nitfFile: File): Elem = {
    val inputXml = Utility.trim(XML.loadFile(nitfFile)).transform(Seq(setVersionToNitf35))
    XhtmlToNitfTransformer(inputXml.toElem())
  }
}

object NitfValidator {
  private val setVersionToNitf35 = rewriteRule("Set version to NITF 3.5") {
    case e: Elem if e.label == "nitf" =>
      e.copy(scope = NamespaceBinding(null, "http://iptc.org/std/NITF/2006-10-18/", TopScope))
        .withAttribute(e.prefix, "version", "-//IPTC//DTD NITF 3.5//EN")
  }
}
