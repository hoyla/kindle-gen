package com.gu.kindlegen

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import scala.xml.Elem


object ArticleNITF {
  private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)
}

case class ArticleNITF(article: Article) {
  import ArticleNITF._

  def nitf: Elem = {
    <nitf version="-//IPTC//DTD NITF 3.5//EN">
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

  private def body = {
    <body>
      <body.head>
        <hedline>
          <hl1>{article.title}</hl1>
        </hedline>
        <byline>{article.byline}</byline>
        <abstract>{articleAbstract}</abstract>
      </body.head>
      <body.content>{bodyContent}</body.content>
      <body.end/>
    </body>
  }

  private def articleAbstract = scala.xml.Unparsed(article.articleAbstract)
  private def bodyContent = article.bodyBlocks.map(scala.xml.Unparsed.apply) /* TODO convert to NITF blocks*/
}
