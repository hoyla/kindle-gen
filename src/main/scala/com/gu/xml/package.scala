package com.gu.xml

import java.io.{ByteArrayInputStream, StringReader}

import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{Schema, SchemaFactory}
import javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI

import scala.util.Try
import scala.util.control.NonFatal
import scala.xml.{PrettyPrinter => _, _}
import scala.xml.transform.{RewriteRule, RuleTransformer}

object `package` {
  def xmlSource(xmlContents: Array[Byte]): Source =
    new StreamSource(new ByteArrayInputStream(xmlContents))

  def xmlSource(xmlContents: String): Source =
    new StreamSource(new StringReader(xmlContents))

  /** Returns a schema that can be used to validate XML.
    *
    * The schema sources must be in a topological order, i.e. a schema file must precede other files that refer to it.
    * For example, if your schema references "xml.xsd", then the call should look like:
    * {{{
    * val xmlSchema: Source = xmlSource("xml.xsd")
    * val mySchema: Source = xmlSource("my.xsd")
    * validateXml(???, xmlSchema, mySchema)
    * }}}
    */
  def xmlSchema(xsdSources: Seq[Source]): Schema =
    SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI).newSchema(xsdSources.toArray)

  /** Creates a [[scala.xml.transform.RewriteRule]] that can be used to transform nodes.
    *
    * @see [[RichNode.transform]]
    * @see [[RichSeqOfNodes.transformAll]]
    */
  def rewriteRule(ruleName: String)(pf: PartialFunction[Node, Seq[Node]]): RewriteRule = new RewriteRule {
    override def transform(n: Node): Seq[Node] = {
      try {
        pf.applyOrElse(n, identity[Seq[Node]])
      } catch {
        case NonFatal(error) =>
          throw new TransformationException(s"""Failed to apply the transformation "$ruleName" on $n""", error)
      }
    }

    override def toString(): String = ruleName
  }

  /** Utility operations for attributes */
  implicit class RichMetaData(val metaData: MetaData) extends AnyVal {
    def without(unwantedAttributes: Seq[String]): MetaData = unwantedAttributes.foldLeft(metaData)(_ remove _)
  }

  implicit class RichNode(val node: Node) extends AnyVal {
    /** Returns all the ''unprefixed keys'' of all attributes attached to this node.
      *
      * The result of this method is different than that of [[scala.xml.MetaData.asAttrMap]]
      * in that the keys returned by this method _do not_ contain prefixes.
      *
      * This method is also more efficient than calling {{{node.attributes.asAttrMap.keys}}}
      * because it doesn't serialise the values.
      */
    def attributeKeys: Iterable[String] =
      node.attributes.map(_.key)

    /** Checks if any ''direct'' child has the specified label */
    def hasChildrenWithLabel(childLabel: String): Boolean =
      hasChildrenMatching(_.label == childLabel)

    /** Checks if any ''direct'' child has any of the specified labels.
      * Example usage: {{{node.hasChildren(Set("p", "span"))}}}
      */
    def hasChildrenWithLabels(childLabels: String => Boolean): Boolean =
      hasChildrenMatching(c => childLabels(c.label))

    /** Checks if any ''direct'' child matches the ''predicate'' */
    def hasChildrenMatching(predicate: Node => Boolean): Boolean =
      node.child.exists(predicate)

    /** Transforms the node using the specified rules.
      * @see [[rewriteRule]]
      */
    def transform(rules: Seq[RewriteRule]): Seq[Node] = {
      new RuleTransformer(rules: _*).transform(node)
    }

    def transformNode(rules: Seq[RewriteRule]): Node = {
      val transformed = transform(rules)
      transformed.length match {
        case 1 => transformed.head
        case 0 => throw new TransformationException(s"Found no nodes after transforming $node")
        case n => throw new TransformationException(s"Transformation resulted in $n nodes:\n${transformed.mkString("\n")}")
      }
    }

    def replaceDescendantsOrSelf(replacement: PartialFunction[Node, Node]): Seq[Node] = node match {
      case n if replacement.isDefinedAt(n) => replacement(n)
      case e: Elem if e.descendant.exists(replacement.isDefinedAt) =>
        e.copy(child = e.child.flatMap(_.replaceDescendantsOrSelf(replacement)))
      case n => n
    }

    /** Creates an element that matches this node, unless this node is a [[scala.xml.SpecialNode]]. */
    def toElem: Option[Elem] = node match {
      case e: Elem => Some(e)
      case _ => Try(toElem()).toOption
    }

    /** Creates an element that matches this node with the specified properties adjusted.
      *
      * @throws IllegalArgumentException if this node is a [[scala.xml.SpecialNode]]
      */
    def toElem(prefix: String = node.prefix,
               label: String = node.label,
               attributes: MetaData = node.attributes,
               scope: NamespaceBinding = node.scope,
               minimizeEmpty: Boolean = node.isEmpty,
               child: Seq[Node] = node.child): Elem = {
      def illegal(description: String) =
        throw new IllegalArgumentException(s"An element cannot be created from $description")
      node match {
        case _: Group => illegal("a Group")
        case _: SpecialNode => illegal(s"${node.label}($node)")
        case _ =>
          Elem(prefix, label, attributes, scope, minimizeEmpty, child: _*)
      }
    }
  }

  implicit class RichElem(val elem: Elem) extends AnyVal {
    def withAttributes(attributes: Attribute*): Elem = attributes.foldLeft(elem)(_ % _)
    def withAttribute(prefix: String, key: String, value: String): Elem = withAttributes(Attribute(prefix, key, value, Null))
    def withoutAttributes(unwanted: String*): Elem = elem.copy(attributes = elem.attributes.without(unwanted))

    // the following methods can be adapted to work on nodes by using Elem(node.prefix, ...) instead of using elem.copy()

    /** Wraps contiguous block of children, where each element ''matches'', with a ''wrapper''.
      *
      * @param matches a predicate that identifies which children are to be wrapped
      * @param wrapper a template to be used for wrapping
      * @return a copy of ''parent'' with contiguous blocks of matching children wrapped
      */
    def wrapChildren(matches: Node => Boolean, wrapper: Elem): Elem =
      elem.copy(child =
        elem.child.adaptPartitions(matches,
          adaptMatching = wrappables => wrapper.copy(child = wrappables)
      ))

    /** Extracts each direct child that ''matches'' to become a sibling to ''parent''.
      * This is equivalent to wrapping contiguous blocks of direct children that ''do not'' match into copies of ''parent''.
      *
      * @param matches a predicate that identifies which children are to be extracted
      * @return a sequence of nodes that either match the condition or are a ''parent'' themselves
      */
    def unwrapChildren(matches: Node => Boolean): NodeSeq =
      elem.child.adaptPartitions(!matches(_),
        adaptMatching = wrappables => elem.copy(child = wrappables)
      )

    def toXmlBytes(encoding: String = "UTF-8")(implicit prettifier: scala.xml.PrettyPrinter): Array[Byte] =
      toXmlString(encoding)(prettifier).getBytes(encoding)

    def toXmlString(encoding: String = "UTF-8")(implicit prettifier: scala.xml.PrettyPrinter): String =
      s"""<?xml version="1.1" encoding="UTF-8"?>\n""" +  // s"" required to evaluate "\n" as a newline
        elem.prettyPrint(prettifier)
  }

  implicit class RichSeqOfNodes(val nodes: Seq[Node]) extends AnyVal {
    type NodeProcessor = Seq[Node] => Seq[Node]

    def prettyPrint(indent: Int, maxLineWidth: Int): String =
      prettyPrint(new PrettyPrinter(maxLineWidth, indent))

    def prettyPrint(implicit prettier: scala.xml.PrettyPrinter): String =
      nodes.map(prettier.format(_)).mkString("\n")

    def transformAll(rules: Seq[RewriteRule]): Seq[Node] =
      new RuleTransformer(rules: _*).transform(nodes)

    /** Partitions a sequence of nodes according to the ''matches'' predicate, adapts them as required,
      * and then combines them again.
      *
      * Nodes are divided into contiguous sections of nodes that either match or don't match the predicate.
      * Each contiguous section is passed to ''adaptMatching'' or ''adaptUnmatching''.
      */
    def adaptPartitions(matches: Node => Boolean,
                        adaptMatching: NodeProcessor = identity,
                        adaptUnmatching: NodeProcessor = identity): Seq[Node] = {
      def adaptUnlessEmpty(partition: Seq[Node], adapt: NodeProcessor): Seq[Node] =
        if (partition.isEmpty) partition else adapt(partition)

      val (before, rest) = nodes.span(!matches(_))
      val (middle, after) = rest.span(matches)

      adaptUnlessEmpty(before, adaptUnmatching) ++
        adaptUnlessEmpty(middle, adaptMatching) ++
        adaptUnlessEmpty(after, _.adaptPartitions(matches, adaptMatching = adaptMatching, adaptUnmatching = adaptUnmatching))
    }
  }

  implicit val defaultPrettyPrinter: PrettyPrinter = new PrettyPrinter(width = 150, step = 3)

  object TrimmingPrinter extends PrettyPrinter(width = Int.MaxValue, step = 0, minimizeEmpty = true) {
    override def format(n: Node, pscope: NamespaceBinding, sb: StringBuilder): Unit = {
      sb.append(NodeSeq.fromSeq(trimProper(n)).toString)
    }

    // copied from https://github.com/scala/scala-xml/pull/113/files
    // TODO use Utility.trimProper when scala-xml v1.1.1 is released
    private def trimProper(x: Node): Seq[Node] = x match {
      case Elem(pre, lab, md, scp, child@_*) =>
        val children = combineAdjacentTextNodes(child) flatMap trimProper
        Elem(pre, lab, md, scp, children.isEmpty, children: _*)
      case Text(s) =>
        new TextBuffer().append(s).toText
      case _ =>
        x
    }

    private def combineAdjacentTextNodes(nodes: Seq[Node]): Seq[Node] = {
      nodes.foldRight(Seq.empty[Node]) {
        case (Text(left), Text(right) +: accMinusLast) => Text(left + right) +: accMinusLast
        case (n, acc) => n +: acc
      }
    }
  }
}
