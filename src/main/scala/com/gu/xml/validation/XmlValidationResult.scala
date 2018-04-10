package com.gu.xml.validation

import scala.xml.SAXParseException


object XmlValidationResult {
  val Successful = XmlValidationResult(Seq.empty)
}

final case class XmlValidationResult(issues: Seq[XmlValidationIssue]) {
  import XmlIssueLevel._

  /** Returns `true` if there are no errors; ignores warnings */
  def isSuccessful: Boolean = fatalErrors.isEmpty && errors.isEmpty

  def errors: Seq[XmlValidationIssue] = issues(Error)
  def fatalErrors: Seq[XmlValidationIssue] = issues(Fatal)
  def warnings: Seq[XmlValidationIssue] = issues(Warning)
  def issues(level: XmlIssueLevel.Value): Seq[XmlValidationIssue] = issues.filter(_.level == level)

  def withIssue(problem: SAXParseException, level: XmlIssueLevel.Value): XmlValidationResult =
    copy(issues = issues :+ XmlValidationIssue(problem, level))

  override def toString: String = s"XmlValidationResult" +
    s"(successful: $isSuccessful, ${fatalErrors.length} fatal errors, ${errors.length} errors, ${warnings.length} warnings)"
}

object XmlIssueLevel extends Enumeration {
  /** Issues that are not serious enough to stop parsing or indicate an error in the document's validity.
    *
    * Examples include (but are not limited to):
    *   - Duplicate entity definition
    *   - Duplicate attribute definition (in the DTD)
    *   - Undeclared element
    *   - Incorrectly formatted `schemaLocation`
    *   - Failure to read schema document
    */
  val Warning = Value("Warning")

  /** A violation of the rules of the XML specification or the rules of the validation schema. */
  val Error = Value("Error")

  /** Errors in the syntax of the XML document or invalid byte sequences for a given encoding. */
  val Fatal = Value("Fatal Error")
}

final case class XmlValidationIssue(problem: SAXParseException, level: XmlIssueLevel.Value) {
  override def toString: String = s"[$level]: $problem"
}
