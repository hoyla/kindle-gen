package com.gu.kindlegen

import scala.xml.Utility

import org.scalatest.FunSpec
import org.scalatest.Matchers._

import com.gu.kindlegen.TestContent._
import com.gu.xml.XmlUtils._


class ArticleNITFSuite extends FunSpec {

  describe("ArticleNITF") {

    val simpleArticle = Article(
      section = Section(id = "theguardian/mainsection/international", title = "International", link = ExampleLink),
      newspaperPageNumber = 2,
      title = "my title",
      docId = "section/date/title",
      link = ExampleLink,
      pubDate = ExampleOffsetDate,
      byline = "my name",
      articleAbstract = "article abstract",
      bodyBlocks = Seq("content"),
      mainImage = None
    )

    it("produces simple NITF") {
      val expectedOutput =
        <nitf version="-//IPTC//DTD NITF 3.5//EN">
          <head>
            <title>my title</title>
            <docdata management-status="usable">
              <doc-id id-string="section/date/title"/>
              <urgency ed-urg="2"/>
              <date.release norm="2017-07-24T00:00:00Z"/>
              <doc.copyright holder="guardian.co.uk"/>
            </docdata>
            <pubdata type="print" date.publication="2017-07-24T00:00:00Z"/>
          </head>
          <body>
            <body.head>
              <hedline>
                <hl1>my title</hl1>
              </hedline>
              <byline>my name</byline>
              <abstract>article abstract</abstract>
            </body.head>
            <body.content>content</body.content>
            <body.end/>
          </body>
        </nitf>

      assertEquivalentXml(ArticleNITF(simpleArticle).nitf, expectedOutput)
    }

    it("handles XHTML tags") {
      val content = <p>abc</p> ++ <p>an <em>emphasised</em> word</p>

      val article = simpleArticle.copy(bodyBlocks = Seq(content.mkString))
      val nitf = Utility.trim(ArticleNITF(article).nitf)
      val body = (nitf \\ "body.content").head.nonEmptyChildren

      body.mkString shouldBe content.mkString
    }
  }
}
