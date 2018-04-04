package com.gu.nitf

import java.io.File
import java.nio.file.{Files, Paths}

import scala.collection.JavaConverters._
import scala.xml._
import scala.xml.transform._

import org.scalatest.FunSpec

import com.gu.xml.XmlUtils._

class NitfValidator extends FunSpec {
  import NitfValidator._

  /* known issues:
       - some weather files have no content
         - 20180117.0111.moved/122_theguardianweather_weather_world.nitf
         - 20180118.0111.moved/115_theguardianweather_weather_world.nitf
         - 20180119.0111.moved/133_theguardianweather_weather_world.nitf
       - stray <li> in 20180117.0111.moved/085_leave-campaigns-350m-claim-was-too-low-says-boris-johnson.nitf
       - XML entity ("Information Separator 3" control char!) in 20180116.0111.moved/060_hm-stores-in-south-afrtica-trashed-by-protesters-after-racist-ad.nitf
       - empty <ul> in 20180223.0111.moved/073_freehold-on-disputed-birmingham-leasehold-flats-goes-on-sale.nitf
       - <ul> with <p> children in 20180217.0111.moved/024_barry-bennell-abuse-manchester-city-crewe.nitf
       - <li> without <ul> or <ol> in
         - 20180308.0111.moved/017_disgraced-ex-co-op-bank-boss-paul-flowers-crystal-methodist-banned-from-financial-services.nitf
         - 20180302.0111.moved/010_judges-told-to-limit-observers-if-witness-has-to-remove-veil.nitf
  */

  private val basePath = Paths.get("../kindle-publications-extracted/feeds").toRealPath()
  private val invalidXmlFiles = Set(
    // empty content / wrong XML
    "20180117.0111.moved/122_theguardianweather_weather_world.nitf",
    "20180118.0111.moved/115_theguardianweather_weather_world.nitf",
    "20180119.0111.moved/133_theguardianweather_weather_world.nitf",
    // misplaced </figure> without an opening tag
    "20171029.0111.moved/086_mysterious-object-detected-speeding-past-the-sun-could-be-from-another-solar-system-a2017-u1.nitf",
    "20171213.0111.moved/038_astronomers-to-check-interstellar-body-for-signs-of-alien-technology.nitf"
  ).map(basePath.resolve)

  Files.walk(basePath).iterator.asScala
    .filter(_.toString.endsWith(".nitf"))
    .filterNot(invalidXmlFiles)
  //.filter(_.getName(5).toString.startsWith("201803"))
    .foreach { nitfFilePath =>
      describe("NITF file " + nitfFilePath) {
        it("should match the schema") {
          try {
            validateFile(nitfFilePath.toFile)
          } catch {
            case e: org.xml.sax.SAXParseException =>
              cancel("XML file is invalid!", e)
          }
        }
      }
    }

  private def validateFile(nitfFile: File): Unit = {
    val xml = transform(Utility.trim(XML.loadFile(nitfFile)))
    validateXml(xml, "kpp-nitf-3.5.7.xsd")
  }
}

object NitfValidator {
  private def transform = new RuleTransformer(
    setVersionToNitf35,
    convertOrRemoveTags,
    convertMisplacedLists,
    removeUnsupportedAttributes,
    unwrapTables,
    wrapBlockContentText,
    wrapElements
  )

  private val setVersionToNitf35 = rewriteRule("set version to NITF 3.5") {
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
    rewriteRule("Convert misplaced <li> to <p>") {
      case e: Elem if isList(e) && e.hasChildren(nonListItem) =>
        if (e.child.forall(nonListItem))
          e.child  // remove the extraneous list tag
        else
          wrapChildren(e, nonListItem, e.copy(label = "li", attributes = Null, child = Nil))
      case e: Elem if !isList(e) && e.hasChildren("li") =>
        e.copy(child = adaptPartitions(e.child, isListItem,
          adaptMatching = _.map(n => Elem(n.prefix, "p", n.attributes, n.scope, true, n.child: _*)))
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
    val blockContentElements = Set("abstract", "block")
    val blockContentTags = Set(
      "p", "hl2", "h3", "h4", "h5", "h6", "table", "nitf-table", "media", "ol", "ul", "dl", "bq", "fn", "note", "pre", "hr", "content"
    )
    val nonBlockContentTag = (x: Node) => !blockContentTags.contains(x.label)
    rewriteRule("Wrap text (and enriched text) in non-mixed elements") {
      case e: Elem if blockContentElements.contains(e.label) && e.hasChildren(nonBlockContentTag) =>
        wrapChildren(e, nonBlockContentTag,
          wrapper = e.copy(label = "p", attributes = Null, child = Nil))
    }
  }

  private val wrapElements = rewriteRule("Wrap elements") {
    case e: Elem if e.label == "blockquote" =>
      // <blockquote>text</blockquote> => <bq><block>text</block></bq>
      // will need to go through [[wrapBlockContentText]] to wrap the text into a paragraph
      e.copy(label = "bq", child = wrapBlockContentText(e.copy(label = "block")))
    case e: Elem if e.label != "content" && e.hasChildren("img") =>
      // <img /> => <content><img /></content>
      e.copy(child = e.child.map {
        case c: Elem if c.label == "img" => c.copy(label = "content", attributes = Null, child = c)
        case c => c
      })
  }

  private val unwrapTables = rewriteRule("Unwrap tables") {
    case e: Elem if e.label != "block" && e.hasChildren("table") =>
      unwrapChildren(e, _.label == "table")
  }

  private def rewriteRule(ruleName: String)(pf: PartialFunction[Node, Seq[Node]]): RewriteRule = new RewriteRule {
    override def transform(n: Node): Seq[Node] = pf.applyOrElse(n, identity[Seq[Node]])
    override def toString(): String = ruleName
  }

  private def wrapChildren(parent: Elem, matches: Node => Boolean, wrapper: Elem): Seq[Node] =
    parent.copy(child =
      adaptPartitions(parent.child, matches,
        adaptMatching = wrappables => wrapper.copy(child = wrappables)
      )
    )

  private def unwrapChildren(parent: Elem, matches: Node => Boolean): Seq[Node] =
    adaptPartitions(parent.child, !matches(_),
      adaptMatching = wrappables => parent.copy(child = wrappables)
    )
  private type NodeProcessor = Seq[Node] => Seq[Node]
  /** Partitions a sequence of nodes according to the ''matcher'' predicate, adapts them as required,
    * and then combines them again.
    *
    * Nodes are divided into contiguous sections of nodes that either match or don't match the predicate.
    * Each contiguous section is passed to ''adaptMatching'' or ''adaptUnmatching''.
    */
  private def adaptPartitions(nodes: Seq[Node],
                              matches: Node => Boolean,
                              adaptMatching: Seq[Node] => Seq[Node] = identity,
                              adaptUnmatching: Seq[Node] => Seq[Node] = identity): Seq[Node] = {
    def adaptUnlessEmpty(partition: Seq[Node], adapt: Seq[Node] => Seq[Node]): Seq[Node] =
      if (partition.isEmpty) partition else adapt(partition)

    val before = nodes.takeWhile(!matches(_))
    val middle = nodes.drop(before.size).takeWhile(matches)
    val after  = nodes.drop(before.size + middle.size)

    adaptUnlessEmpty(before, adaptUnmatching) ++
      adaptUnlessEmpty(middle, adaptMatching) ++
      adaptUnlessEmpty(after, adaptPartitions(_, matches, adaptMatching = adaptMatching, adaptUnmatching = adaptUnmatching))
  }
}
