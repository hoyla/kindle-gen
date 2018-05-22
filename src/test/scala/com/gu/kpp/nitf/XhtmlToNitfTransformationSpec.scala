package com.gu.kpp.nitf

import java.io.File
import java.nio.file.{Path, Paths}

import scala.xml._

import org.scalatest.FunSpec
import org.scalatest.Matchers._

import com.gu.xml._
import com.gu.xml.XmlUtils._


class XhtmlToNitfTransformationSpec extends FunSpec {
  import XhtmlToNitfTransformationSpec._

  filesToTest.foreach(test)

  protected def filesToTest: TraversableOnce[Path] =
    Seq(Paths.get(resource("xhtml-example.nitf").toURI))

  protected def test(nitfFilePath: Path): Unit = {
      describe("NITF file " + nitfFilePath) {
        it("should match the schema") {
          try {
            val xml = loadAndTransform(nitfFilePath.toFile)
            validateXml(xml, resource("kpp-nitf-3.5.7.xsd").toURI)

            // the transformer should be idempotent: applying the transformation to valid NITF should do nothing
            XhtmlToNitfTransformer(xml) shouldBe xml
          } catch {
            case e: org.xml.sax.SAXParseException =>
              e.printStackTrace()
              cancel("XML file is invalid! " + e.getMessage, e)
          }
        }
      }
  }

  protected def loadAndTransform(nitfFile: File): Elem = {
    val inputXml = Utility.trim(XML.loadFile(nitfFile)).transform(Seq(setVersionToNitf35))
    XhtmlToNitfTransformer(inputXml.toElem())
  }
}

object XhtmlToNitfTransformationSpec {
  private val setVersionToNitf35 = rewriteRule("Set version to NITF 3.5") {
    case e: Elem if e.label == "nitf" =>
      e.copy(scope = NamespaceBinding(null, "http://iptc.org/std/NITF/2006-10-18/", TopScope))
        .withAttribute(e.prefix, "version", "-//IPTC//DTD NITF 3.5//EN")
  }
}
