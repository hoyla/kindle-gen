package com.gu.kindlegen

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import scala.util.{Failure, Try}
import scala.xml._

import org.jsoup.Jsoup
import org.jsoup.nodes.Document.OutputSettings.Syntax
import org.jsoup.nodes.Entities.EscapeMode
import org.jsoup.safety.{Cleaner, Whitelist}

import com.gu.kpp.nitf.XhtmlToNitfTransformer
import com.gu.xml._


object ArticleNITF {
  val Version = "-//IPTC//DTD NITF 3.5//EN"
  private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)

  private val cleaner = new Cleaner(Whitelist.relaxed
    .addProtocols("a", "href", "#")
    .removeProtocols("a", "href", "ftp", "http", "https", "mailto")
  )

  def qualify(nitf: Elem): Elem =
    nitf.copy(scope = NamespaceBinding(null, "http://iptc.org/std/NITF/2006-10-18/", TopScope))

  private def htmlToXhtml(html: String): NodeSeq = {
    val saferHtml = if (html.exists(_.isControl)) html.filterNot(_.isControl) else html

    val document = cleaner.clean(Jsoup.parseBodyFragment(saferHtml.trim))
    document.outputSettings
      .syntax(Syntax.xml)
      .escapeMode(EscapeMode.xhtml)
      .prettyPrint(false)  // actual pretty-printing will be applied to the XML


    val xhtml = document.body.outerHtml
    Try(NodeSeq.fromSeq(XML.loadString(xhtml).child)).recoverWith {
      case e: org.xml.sax.SAXParseException =>
        Failure(new RuntimeException(s"Failed to parse XHTML: $e\n$xhtml", e))
    }.get
  }
}

case class ArticleNITF(article: Article) {
  import ArticleNITF._

  def nitf: Elem = {  // no xmlns - Amazon's NITF processor doesn't support it
    <nitf version={Version}>
      {head}
      {body}
    </nitf>
  }

  private def head = {
    val pubDate = formatter.format(article.pubDate)
    <head>
      <title>{article.title}</title>
      <docdata management-status="usable">
        <doc-id id-string={article.docId}/>
        <urgency ed-urg="2"/>
        <date.release norm={pubDate}/>
        <doc.copyright holder="guardian.co.uk"/>
      </docdata>
      <pubdata type="print" date.publication={pubDate}/>
    </head>
  }

  private def body = XhtmlToNitfTransformer(Utility.trim {
    <body>
      <body.head>
        <hedline>
          <hl1>{article.title}</hl1>
        </hedline>
        <byline>{article.byline}</byline>
        <abstract>{articleAbstract}</abstract>
      </body.head>
      <body.content>
        {mainImage ++ bodyContent}
      </body.content>
      <body.end/>
    </body>
  }.toElem.get)

  private def articleAbstract = htmlToXhtml(article.articleAbstract)
  private def bodyContent = article.bodyBlocks.map(html => <block>{htmlToXhtml(html)}</block>)
  private def mainImage: Option[Elem] = article.mainImage.map { image =>
    <content>
      <img src={image.link.source}>
        {image.caption.getOrElse("")} {image.credit.getOrElse("")}  {/* TODO should we show article.trailText? */}
      </img>
    </content>
  }
}
