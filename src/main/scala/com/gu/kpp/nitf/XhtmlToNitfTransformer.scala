package com.gu.kpp.nitf

import scala.xml._
import scala.xml.transform.RewriteRule

import com.gu.nitf.HtmlToNitfConfig
import com.gu.xml._


object XhtmlToNitfTransformer extends XhtmlToNitfTransformer(KindleHtmlToNitfConfig)

class XhtmlToNitfTransformer(config: HtmlToNitfConfig) {
  /** Transforms an XHTML document into valid NITF <body.content>.
    *
    * @param xhtml an element representing an XHTML document
    * @return the element transformed to match NITF specs
    */
  def apply(xhtml: Elem): Seq[Node] = {
    xhtml.transform(transformationRules)
  }

  // the order of these rules is important
  private def transformationRules = Seq(
    removeControlCharacters,
    convertHtmlTags,
    convertMisplacedLists,
    removeUnsupportedAttributes,
    removeTagsMissingRequiredAttributes,
    replaceBreaksInAbstract,
    wrapTextIntoBlockContent,
    wrapSpecialElements,
    unwrapTopLevelTags
  )

  private val removeControlCharacters = {  // temporary rule until https://github.com/scala/scala-xml/pull/203 is fixed
    val unwanted = (c: Char) => c.isControl && c != '\n' && c != '\r' && c != '\t' && c != '\u0085' /* NEXT LINE (NEL) */

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

  /** Maps HTML tags to their NITF equivalents, removing unwanted ones.
    *
    * Unsupported HTML tags are discarded but their contents are preserved.
    */
  private val convertHtmlTags = {
    val tagMapping = config.equivalentNitfTag
    val unwantedTags = config.blacklist
    val nonEmptyTags = config.nitf.nonEmptyTags
    val supportedTags = config.supportedNitfTags

    rewriteRule("Convert HTML tags") {
      case n if unwantedTags.contains(n.label) => Nil
      case e: Elem if nonEmptyTags.contains(e.label) && e.child.isEmpty => Nil
      case e: Elem if tagMapping.contains(e.label) => e.copy(label = tagMapping(e.label))
      case e: Elem if !supportedTags(e.label) => e.child
    }
  }

  /** Fixes badly-formatted HTML lists.
    *
    * Some articles may contain text directly inside HTML lists.
    * If the list only contains text, then we convert it to a paragraph.
    * If the list is mixed, then we wrap the text in list items.
    */
  private val convertMisplacedLists = {
    val isList = (x: Node) => x.label == "ol" || x.label == "ul"
    val isListItem = (x: Node) => x.label == "li"
    val nonListItem = (x: Node) => !isListItem(x)
    rewriteRule("Convert misplaced <li>, <ol> and <ul> to <p>") {
      case e: Elem if isList(e) && e.hasChildrenMatching(nonListItem) =>
        if (e.child.forall(nonListItem))
          e.copy(label = "p")  // Google Chrome renders lists without list items as an indented paragraph
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
      "management-status", "ed-urg", "id-string", "holder", "norm", "date.publication"
    )
    rewriteRule("Remove unsupported attributes") {
      case e: Elem if !e.attributeKeys.forall(supportedAttributes) =>
        e.withoutAttributes((e.attributeKeys.toSet -- supportedAttributes).toSeq: _*)
    }
  }

  /** Remove the tags that are useless without their attributes (e.g. an anchor with neither a link nor an id nor a name) */
  private val removeTagsMissingRequiredAttributes = {
    val tags = Set("a")
    rewriteRule("Remove tags missing required attributes") {
      case e: Elem if tags.contains(e.label) && e.attributes.isEmpty =>
        e.unwrapChildren(_ => true)
    }
  }

  private val replaceBreaksInAbstract = {
    rewriteRule("Replace <br/> in <abstract> with a dash") {
      case e: Elem if e.label == "abstract" =>
        e.replaceDescendantsOrSelf { case n if n.label == "br" => Text(" – ") }
    }
  }

  private val enrichedTextOnlyParentTags = config.nitf.enrichedTextOnlyParentTags
  private val enrichedTextParentTags = config.nitf.enrichedTextParentTags
  private def enrichedTextNode(node: Node) = config.nitf.enrichedTextTags.contains(node.label)
  private def nonEnrichedTextNode(node: Node) = !enrichedTextNode(node)
  private def justSpaces(node: Node) = node.label == "#PCDATA" && node.text.forall(_.isWhitespace)

  private val wrapTextIntoBlockContent: RewriteRule = {
    rewriteRule("Wrap text (and enriched text) in block (non-mixed) elements") {
      // <bq>...</bq> => <bq><block>...</block></bq>
      case e: Elem if e.label == "bq" && e.hasChildrenMatching(_.label != "block") =>
        val block = e.copy(label = "block", child = e.child)
        e.copy(child = apply(block))  // apply all transformations to the blockquote contents

      case e: Elem if !enrichedTextParentTags(e.label) &&
                      e.hasChildrenMatching(n => enrichedTextNode(n) && !justSpaces(n)) =>
        e.wrapChildren(enrichedTextNode, e.copy(label = "p", attributes = Null))
    }
  }

  private val unwrapTopLevelTags = {
    rewriteRule("Unwrap block contents from non-block parents") {
      case e: Elem if e.label == "block" && e.hasChildrenWithLabel("block") =>
        e.unwrapChildren(_.label == "block")  // special case for html tags mapped to blocks

      case e: Elem if enrichedTextOnlyParentTags(e.label) && e.hasChildrenMatching(nonEnrichedTextNode) =>
        e.unwrapChildren(nonEnrichedTextNode)
    }
  }

  private val wrapSpecialElements = rewriteRule("Wrap special elements") {
    case e: Elem if e.label != "content" && e.hasChildrenWithLabel("img") =>
      e.wrapChildren(_.label == "img", e.copy(label = "content", attributes = Null))
  }
}
