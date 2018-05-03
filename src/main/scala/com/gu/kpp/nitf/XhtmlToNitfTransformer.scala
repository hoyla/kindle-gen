package com.gu.kpp.nitf

import scala.xml._

import com.gu.xml._

object XhtmlToNitfTransformer {
  /** Transforms an XHTML document into valid NITF <body.content>.
    *
    * @param xhtml an element representing an XHTML document
    * @return the element transformed to match NITF specs
    */
  def apply(xhtml: Elem): Elem = {
    xhtml.transform(transformationRules).toElem()
  }

  private def transformationRules = Seq(
    removeControlCharacters,
    convertOrRemoveTags,
    convertMisplacedLists,
    removeUnsupportedAttributes,
    removeTagsMissingRequiredAttributes,
    wrapBlockContentText,
    wrapSpecialElements,
    unwrapBlockContentParents
  )

  private val BlockContentParents = Set("abstract", "block", "body.content", "bq")
  private val BlockContentTags = Set(
    "block", "bq", "content", "dl", "fn", "hl2", "h3", "h4", "h5", "h6",
    "hr", "media", "nitf-table", "note", "ol", "p", "pre", "table", "ul"
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

  private val convertOrRemoveTags = {
    // TODO how about <div> -> <block>, <cite>, <code> -> <pre>, <small>, <caption>, tables?
    val mappings = Map("b" -> "strong", "h2" -> "hl2", "i" -> "em", "u" -> "em")
    val unsupportedTags = Set("figure", "span", "sub", "sup")
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

  /** Remove the tags that are useless without their attributes (e.g. an anchor whose link has been removed) */
  private val removeTagsMissingRequiredAttributes = {
    val tags = Set("a")
    rewriteRule("Remove tags missing required attributes") {
      case e: Elem if tags.contains(e.label) && e.attributes.isEmpty =>
        e.unwrapChildren(_ => true)
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
