package com.gu.xml

import scala.xml.{Elem, NamespaceBinding, Node}


/** A thread-safe pretty printer that respects the setting of `minimizeEmpty` even for tags with lengthy attributes. */
class PrettyPrinter(val width: Int, val step: Int, val minimizeEmpty: Boolean = false)
    extends scala.xml.PrettyPrinter(width, step, minimizeEmpty) {

  override protected def traverse(node: Node, pscope: NamespaceBinding, indent: Int): Unit = {
    node match {
      case e: Elem if e.child.isEmpty && (minimizeEmpty || e.minimizeEmpty) =>
        makeBox(indent, leafTag(e))

      case _ =>
        super.traverse(node, pscope, indent)
    }
  }

  // make this class thread-safe
  override def format(n: Node, pscope: NamespaceBinding, sb: StringBuilder): Unit =
    new PrettyPrinter(width, step, minimizeEmpty).unsafeFormat(n, pscope, sb)

  private def unsafeFormat(n: Node, pscope: NamespaceBinding, sb: StringBuilder): Unit =
    super.format(n, pscope, sb)
}
