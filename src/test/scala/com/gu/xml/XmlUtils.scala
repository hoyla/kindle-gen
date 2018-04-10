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

  def writeTemporaryFile(fileName: String, xmlContents: NodeSeq): Path = {
    val outputDir = Files.createDirectories(Paths.get("target/tmp"))
    Files.write(outputDir.resolve(s"$fileName.xml"), prettyPrint(xmlContents).getBytes("UTF8"))
  }

  def prettify(xml: Node): Elem = {
    XML.load(new StringReader(prettyPrint(xml)))
  }

  def prettyPrint(n: NodeSeq): String = {
    val printer = new PrettyPrinter(200, 2)
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
