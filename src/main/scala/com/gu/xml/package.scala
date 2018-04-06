package com.gu.xml

import scala.xml._
import scala.xml.transform.{RewriteRule, RuleTransformer}

object `package` {
  /** Creates a [[scala.xml.transform.RewriteRule]] that can be used to transform nodes.
    *
    * @see [[RichNode.transform]]
    * @see [[RichSeqOfNodes.transformAll]]
    */
  def rewriteRule(ruleName: String)(pf: PartialFunction[Node, Seq[Node]]): RewriteRule = new RewriteRule {
    override def transform(n: Node): Seq[Node] = pf.applyOrElse(n, identity[Seq[Node]])
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
    def transform(rules: RewriteRule*): Node = new RuleTransformer(rules: _*).apply(node)
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
    def unwrapChildren(matches: Node => Boolean): Seq[Node] =
      elem.child.adaptPartitions(!matches(_),
        adaptMatching = wrappables => elem.copy(child = wrappables)
      )
  }

  implicit class RichSeqOfNodes(val nodes: Seq[Node]) extends AnyVal {
    type NodeProcessor = Seq[Node] => Seq[Node]

    def transformAll(rules: RewriteRule*): Seq[Node] =
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
}
