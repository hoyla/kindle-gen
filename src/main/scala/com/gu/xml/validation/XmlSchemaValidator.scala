package com.gu.xml.validation

import java.io.{ByteArrayInputStream, StringReader}
import java.nio.file.{Files, Path}

import javax.xml.transform.stream.StreamSource
import javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI
import javax.xml.transform.Source
import javax.xml.validation.{Schema, SchemaFactory}

import scala.util.Try
import scala.xml.{NodeSeq, SAXParseException}

import org.xml.sax.ErrorHandler


object XmlSchemaValidator extends XmlSchemaValidator {
  def xmlSource(xmlLocation: Path): Source =
    new StreamSource(new ByteArrayInputStream(Files.readAllBytes(xmlLocation)))
}

/** Methods to validate XML against an XSD */
trait XmlSchemaValidator {
  /** Validates ''xmlContents'' against the specified schema.
    *
    * The schema must be in a topological order, i.e. a schema file must precede other files that refer to it.
    * For example, if your schema references "xml.xsd", then the call should look like:
    * {{{
    * val xmlSchema: Source = xsdSource("xml.xsd")
    * val mySchema: Source = xsdSource("my.xsd")
    * validateXml(???, xmlSchema, mySchema)
    * }}}
    */
  def validateXml(xmlContents: NodeSeq, xsdSources: Source*): XmlValidationResult = {
    val schema = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI).newSchema(xsdSources.toArray)
    validateXml(xmlContents, schema)
  }

  def validateXml(xmlContents: NodeSeq, schema: Schema): XmlValidationResult = {
    val validator = schema.newValidator()
    val errorHandler = issueCollector
    validator.setErrorHandler(errorHandler)

    Try {
      validator.validate(new StreamSource(new StringReader(xmlContents.toString)))
    }.recover {
      case t: SAXParseException => errorHandler.fatalError(t)
    }

    errorHandler.result
  }

  def issueCollector: IssueCollectingSaxErrorHandler = new IssueCollectingSaxErrorHandler {}
}

trait IssueCollectingSaxErrorHandler extends ErrorHandler {
  def result: XmlValidationResult = issues
  protected var issues: XmlValidationResult = XmlValidationResult.Successful

  import XmlIssueLevel._
  override def warning   (problem: SAXParseException): Unit = { issues = issues.withIssue(problem, Warning) }
  override def error     (problem: SAXParseException): Unit = { issues = issues.withIssue(problem, Error) }
  override def fatalError(problem: SAXParseException): Unit = { issues = issues.withIssue(problem, Fatal) }
}
