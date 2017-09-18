package com.gu.kindlegen

import DateUtils._

import scala.xml._
import scala.xml.{ Elem, XML }
import scala.xml.factory.XMLLoader
import org.xml.sax.InputSource

import scala.xml._
import parsing._
import scala.xml.transform.{ RewriteRule, RuleTransformer }

case class ArticleNITF(fileContents: String)

object ArticleNITF {

  // TODO: replace content below with filtered content in NITF
  //  def filterBodyContent(article: Article): String = {
  //    val removeIt = new RewriteRule {
  //      override def transform(n: Node): NodeSeq = n match {
  //        case e: Elem if (e \ "a").text == "href" => NodeSeq.Empty
  //        case n => n
  //      }
  //    }
  //    val bodyContent = article.content
  //    //    bodyContent.
  //    //      replaceAll("""’""", "'"). // converts apostrophe
  //    //      replaceAll("""\<a .*>""", "") // strip links
  //    val bodyXML = XML.loadString(bodyContent)
  //    val filteredXML = new RuleTransformer(removeIt).transform(bodyXML)
  //    filteredXML.toString
  //  }

  def apply(article: Article) = new ArticleNITF(
    fileContents = s"""
     |<?xml version="1.0" encoding="UTF-8"?>
     |<nitf version="-//IPTC//DTD NITF 3.3//EN">
     |<head>
     |<title>${article.title}</title>
     |<docdata management-status="usable">
     |<doc-id id-string="${article.docId}" />
     |<urgency ed-urg="2" />
     |<date.issue norm="${isoDateConverter(article.issueDate)}" />
     |<date.release norm="${isoDateConverter(article.releaseDate)}" />
     |<doc.copyright holder="guardian.co.uk" />
     |</docdata>
     |<pubdata type="print" date.publication="${isoDateConverter(article.pubDate)}" />
     |</head>
     |<body>
     |<body.head>
     |<hedline><hl1>${article.title}</hl1></hedline>
     |<byline>${article.byline}</byline>
     |<abstract>${article.articleAbstract}</abstract>
     |</body.head>
     |<body.content>${article.content}</body.content>
     |<body.end />
     |</body>
     |</nitf>""".stripMargin
  )

}
