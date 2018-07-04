package com.gu.xml.validation

import javax.xml.transform.Source
import javax.xml.validation.Schema

import scala.util.Try
import scala.xml.SAXParseException

import org.xml.sax.ErrorHandler

object XmlSchemaValidator extends XmlSchemaValidator

/** Methods to validate XML against an XSD */
trait XmlSchemaValidator {
  def validateXml(xmlSource: Source, schema: Schema): XmlValidationResult = {
    val validator = schema.newValidator()
    val errorHandler = issueCollector
    validator.setErrorHandler(errorHandler)

    Try {
      validator.validate(xmlSource)
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
