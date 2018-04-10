package com.gu.xml

import java.io.StringReader
import java.net.{URI, URL}
import java.nio.file.{Files, Path, Paths}

import scala.xml._

import com.github.andyglow.xml.diff._
import org.scalatest.Matchers._
import org.scalatest.xml.XmlMatchers._

import com.gu.xml.validation.XmlSchemaValidator


object XmlUtils {
  def resource(fileName: String): URL =
    Option(Thread.currentThread.getContextClassLoader)
      .getOrElse(this.getClass.getClassLoader)
      .getResource(fileName)

  def writeTemporaryFile(xmlContents: Node, fileName: String, outputSubDir: Path = Paths.get(".")): Path = {
    val outputDir = Files.createDirectories(Paths.get("target", "tmp").resolve(outputSubDir))
    val outputPath = outputDir.toRealPath().resolve(fileName)
    XML.save(filename = outputPath.toString, node = xmlContents, xmlDecl = true, enc = "UTF-8")
    outputPath
  }

  def prettify(xml: Node): Elem = {
    XML.load(new StringReader(prettyPrint(xml)))
  }

  def prettyPrint(n: Seq[Node], maxLineWidth: Int = 200): String = {
    val printer = new PrettyPrinter(maxLineWidth, 2)
    n.map(printer.format(_)).mkString("\n")
  }

  def assertEquivalentXml(actual: Node, expected: Node): Unit = {
    val diff = expected =#= actual
    withClue(diff.toString + "\n" + diff.errorMessage + "\n") {
      prettify(actual) should beXml(prettify(expected), ignoreWhitespace = true)
    }
  }

  def validateXml(xmlContents: NodeSeq, schemaURI: URI): Unit = {
    val schemaPath = Paths.get(schemaURI)
    val xsdSources = Seq(schemaPath.resolveSibling("xml.xsd"), schemaPath).map(XmlSchemaValidator.xmlSource)

    withClue(prettyPrint(xmlContents) + "\n") {
      val validationResult = XmlSchemaValidator.validateXml(xmlContents, xsdSources: _*)
      withClue(validationResult.issues.mkString("\n")) {
        validationResult shouldBe 'successful
      }
    }
  }
}
