package com.gu.xml

import scala.xml.{Elem, Text}

import org.scalatest.FunSpec
import org.scalatest.Matchers._


class XmlSpec extends FunSpec {
  describe("TrimmingPrinter") {
    it("compacts input") {
      val xml = <a> <b>  <c>   </c>    <d/>     </b></a>
      TrimmingPrinter.format(xml) shouldBe "<a><b><c/><d/></b></a>"
    }

    it("maintains spaces inside contiguous text nodes") {
      val xml = <a/>.copy(child = Seq("   ", "Some ", "text", " with", " spaces ", "...", "   ").map(Text.apply))
      TrimmingPrinter.format(xml) shouldBe "<a> Some text with spaces ... </a>"
    }
  }
}
