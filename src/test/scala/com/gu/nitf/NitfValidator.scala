package com.gu.nitf

import java.io.File
import java.nio.file.{Files, Paths}

import scala.collection.JavaConverters._
import scala.xml._

import org.scalatest.FunSpec

import com.gu.xml._
import com.gu.xml.XmlUtils._

class NitfValidator extends FunSpec {
  import NitfValidator._

  private val historicalNitfFiles: Iterator[Path] = {
    val basePath = Paths.get("../kindle-publications-extracted/feeds").toRealPath()
    val invalidXmlFiles = Set(
      // empty content / wrong XML
      "20180117.0111.moved/122_theguardianweather_weather_world.nitf",
      "20180118.0111.moved/115_theguardianweather_weather_world.nitf",
      "20180119.0111.moved/133_theguardianweather_weather_world.nitf",
      "20180120.0111.moved/125_theguardianweather_weather_world.nitf",
      "20180121.0111.moved/201_theguardianweather_weather_world.nitf",
      "20180122.0111.moved/170_theobserverweather_weather_obs.nitf",
      "20180123.0111.moved/107_theguardianweather_weather_world.nitf",
      "20180124.0111.moved/112_theguardianweather_weather_world.nitf",
      "20180125.0111.moved/115_theguardianweather_weather_world.nitf",
      // misplaced </figure> without an opening tag
      "20171029.0111.moved/086_mysterious-object-detected-speeding-past-the-sun-could-be-from-another-solar-system-a2017-u1.nitf",
      "20171213.0111.moved/038_astronomers-to-check-interstellar-body-for-signs-of-alien-technology.nitf"
    ).map(basePath.resolve)

    Files.walk(basePath).iterator.asScala
      .filter(_.toString.endsWith(".nitf"))
      .filterNot(invalidXmlFiles)
  }

  historicalNitfFiles
    .foreach { nitfFilePath =>
      describe("NITF file " + nitfFilePath) {
        it("should match the schema") {
          try {
            validateFile(nitfFilePath.toFile)
          } catch {
            case e: org.xml.sax.SAXParseException =>
              cancel("XML file is invalid! " + e.getMessage, e)
          }
        }
      }
    }

  private def validateFile(nitfFile: File): Node = {
    val xml = Utility.trim(XML.loadFile(nitfFile)).transform(transformRules: _*)
    validateXml(xml, "kpp-nitf-3.5.7.xsd")
    xml
  }
}

object NitfValidator {
  private val BlockContentParents = Set("abstract", "block")
  private val BlockContentTags = Set(
    "bq", "content", "dl", "fn", "hl2", "h3", "h4", "h5", "h6", "hr",
    "media", "nitf-table", "note", "ol", "p", "pre", "table", "ul"
  )

  private def transformRules = Seq(
    removeControlCharacters,
    setVersionToNitf35,
    convertOrRemoveTags,
    convertMisplacedLists,
    removeUnsupportedAttributes,
    wrapBlockContentText,
    wrapSpecialElements,
    unwrapBlockContentParents
  )

  private val removeControlCharacters = {  // temporary rule until https://github.com/scala/scala-xml/pull/203 is fixed
    val unwanted = (c: Char) => c.isControl && c != '\n' && c != '\r' && c != '\t'

    rewriteRule("Remove control characters") {
      case a: Atom[_] if a.text.exists(unwanted) =>
        val cleaned = a.text.filterNot(unwanted)
        a match {
          case x: Text     => Text(cleaned)
          case x: PCData   => PCData(cleaned)
          case x: Unparsed => Unparsed(cleaned)
          case _           => new Atom(cleaned)
        }
    }
  }

  private val setVersionToNitf35 = rewriteRule("Set version to NITF 3.5") {
    case e: Elem if e.label == "nitf" =>
      e.copy(scope = NamespaceBinding(null, "http://iptc.org/std/NITF/2006-10-18/", TopScope))
        .withAttribute(e.prefix, "version", "-//IPTC//DTD NITF 3.5//EN")
  }

  private val convertOrRemoveTags = {
    val mappings = Map("b" -> "strong", "h2" -> "hl2", "i" -> "em", "u" -> "em")  // apparently, Amazon handles some of these
    val unsupportedTags = Set("figure", "span", "sub", "sup")  // TODO should we format such text? (sub and sup can be in <num>)
    val unwantedTags = Set("s", "strike")  // tags that should be removed along with their content
    val nonEmptyTags = Set("note", "abstract", "dl", "fn", "ol", "tr", "ul")  // tags that must contain something
    rewriteRule("Convert or remove tags") {
      case e: Elem if mappings.contains(e.label) => e.copy(label = mappings(e.label))
      case n if unsupportedTags.contains(n.label) => n.child
      case n if unwantedTags.contains(n.label) => Nil
      case e: Elem if nonEmptyTags.contains(e.label) && e.child.isEmpty => Nil
    }
  }

  private val convertMisplacedLists = {
    val isList = (x: Node) => x.label == "ol" || x.label == "ul"
    val isListItem = (x: Node) => x.label == "li"
    val nonListItem = (x: Node) => !isListItem(x)
    rewriteRule("Convert misplaced <li>, <ol> and <ul> to <p>") {
      case e: Elem if isList(e) && e.hasChildrenMatching(nonListItem) =>
        if (e.child.forall(nonListItem))
          e.child  // remove the extraneous list tag
        else
          e.wrapChildren(nonListItem, e.copy(label = "li", attributes = Null, child = Nil))
      case e: Elem if !isList(e) && e.hasChildrenWithLabel("li") =>
        e.copy(child =
          e.child.adaptPartitions(isListItem, adaptMatching = _.map(_.toElem(label = "p")))
        )
    }
  }

  private val removeUnsupportedAttributes = {
    val supportedAttributes = Set(
      "id", "class", "style", "lang",
      "align", "char", "charoff", "valign",
      "src", "credit",
      "type",
      "management-status", "ed-urg", "id-string"
    )
    rewriteRule("Remove unsupported attributes") {
      case e: Elem if !e.attributeKeys.forall(supportedAttributes) =>
        e.withoutAttributes((e.attributeKeys.toSet -- supportedAttributes).toSeq: _*)
    }
  }

  private val wrapBlockContentText = {
    val nonBlockContentTag = (x: Node) => !BlockContentTags.contains(x.label)
    rewriteRule("Wrap text (and enriched text) in non-mixed elements") {
      case e: Elem if BlockContentParents.contains(e.label) && e.hasChildrenMatching(nonBlockContentTag) =>
        e.wrapChildren(nonBlockContentTag,
          wrapper = e.copy(label = "p", attributes = Null, child = Nil))
    }
  }

  private val unwrapBlockContentParents = {
    rewriteRule("Unwrap block content parents") {
      case e: Elem if !BlockContentParents.contains(e.label) && e.hasChildrenWithLabels(BlockContentTags) =>
        e.unwrapChildren(child => BlockContentTags.contains(child.label))
    }
  }

  private val wrapSpecialElements = rewriteRule("Wrap special elements") {
    case e: Elem if e.label == "blockquote" =>
      // <blockquote>text</blockquote> => <bq><block>text</block></bq>
      // will need to go through [[wrapBlockContentText]] to wrap the text into a paragraph
      e.copy(label = "bq", child = wrapBlockContentText(e.copy(label = "block")))
    case e: Elem if e.label != "content" && e.hasChildrenWithLabel("img") =>
      // <img /> => <content><img /></content>
      e.copy(child = e.child.map {
        case c: Elem if c.label == "img" => c.copy(label = "content", attributes = Null, child = c)
        case c => c
      })
  }
}
