package com.gu.xml

import java.io.{ByteArrayInputStream, StringReader}
import java.net.{URI, URL}
import java.nio.file.{Files, Path, Paths}

import javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{Schema, SchemaFactory}

import scala.util.control.NonFatal
import scala.xml._
import scala.xml.parsing.ConsoleErrorHandler

import com.github.andyglow.xml.diff._
import org.scalatest.Matchers._
import org.scalatest.exceptions.{TestCanceledException, TestFailedException}
import org.scalatest.xml.XmlMatchers._
import org.xml.sax.ErrorHandler


object XmlUtils {
  implicit class RichMetaData(val metaData: MetaData) extends AnyVal {
    def without(unwantedAttributes: Seq[String]): MetaData = unwantedAttributes.foldLeft(metaData)(_ remove _)
  }

  implicit class RichElem(val e: Elem) extends AnyVal {
    def attributeKeys: Iterable[String] = e.attributes.map(_.key)
    def hasChildren(childLabels: String*): Boolean = hasChildren(c => childLabels.contains(c.label))
    def hasChildren(predicate: Node => Boolean): Boolean = e.child.exists(predicate)
    def withAttribute(prefix: String, key: String, value: String): Elem = e % Attribute(prefix, key, value, Null)
    def withAttributes(attrs: Attribute*): Elem = attrs.foldLeft(e)(_ % _)
    def withoutAttributes(unwanted: String*): Elem = e.copy(attributes = e.attributes.without(unwanted))
  }

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

  def validateXml(xmlContents: NodeSeq, schemaResourceName: String): Unit =
    validateXml(xmlContents, resource(schemaResourceName).toURI)

  def validateXml(xmlContents: NodeSeq, schemaPath: URI): Unit = {
    val path = Paths.get(schemaPath)
    val xsdSources = Seq(path.resolveSibling("xml.xsd"), path).map(xsdSource)
    val schema = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI).newSchema(xsdSources.toArray)

    withClue(prettyPrint(xmlContents)) {
      val validationErrors = try { validateXml(xmlContents, schema) } catch { case NonFatal(e) => throw new TestFailedException(e, 0) }
      val formattedErrors = if (validationErrors.isEmpty) "" else validationErrors.mkString("\nValidation errors:\n", "\n", "\n")
      formattedErrors shouldBe empty
    }
  }

  def validateXml(xmlContents: NodeSeq, schema: Schema): Iterable[SAXParseException] = {
    val validator = schema.newValidator()
    val errorHandler = new ConsoleErrorHandler with ExceptionCollectingSaxErrorHandler
    validator.setErrorHandler(errorHandler)
    validator.validate(new StreamSource(new StringReader(xmlContents.toString)))
    errorHandler.exceptions
  }

  private def xsdSource(xsdPath: Path): Source =
    new StreamSource(new ByteArrayInputStream(Files.readAllBytes(xsdPath)))

  private trait ExceptionCollectingSaxErrorHandler extends ErrorHandler {
    var exceptions = Seq.empty[SAXParseException]
    abstract override def warning   (ex: SAXParseException): Unit = { collect(ex); super.warning(ex) }
    abstract override def error     (ex: SAXParseException): Unit = { collect(ex); super.error(ex) }
    abstract override def fatalError(ex: SAXParseException): Unit = { collect(ex); super.fatalError(ex) }
    protected def collect(ex: SAXParseException): Unit = { exceptions :+= ex }
  }
}