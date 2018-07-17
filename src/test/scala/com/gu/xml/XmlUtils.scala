package com.gu.xml

import scala.xml._

import com.github.andyglow.xml.diff._
import org.scalatest.Matchers._
import org.scalatest.xml.XmlMatchers._

import com.gu.xml.validation.XmlSchemaValidator


object XmlUtils {

  def prettify(xml: NodeSeq): Elem = {
    XML.loadString(xml.prettyPrint)
  }

  def assertEquivalentXml(actual: Node, expected: Node): Unit = {
    val diff = expected =#= actual
    withClue(diff.toString + "\n" + diff.errorMessage + "\n") {
      // swap expected and actual because `beXml` v2.0.3 reports results in the opposite order
      prettify(expected) should beXml(prettify(actual), ignoreWhitespace = true)
    }
  }

  def validateXml(xmlContents: NodeSeq, schemaContents: Seq[String]): Unit = {
    val xsdSources = schemaContents.map(xmlSource)

    val prettyXml = xmlContents.prettyPrint
    withClue(prettyXml + "\n") {
      val validationResult = XmlSchemaValidator.validateXml(xmlSource(prettyXml.getBytes), xmlSchema(xsdSources))
      withClue(validationResult.issues.mkString("", "\n", "\n")) {
        validationResult shouldBe 'successful
      }
    }
  }
}
