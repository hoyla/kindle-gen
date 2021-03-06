package com.gu.kindlegen

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import scala.collection.JavaConverters._
import scala.collection.concurrent.TrieMap
import scala.util.control.NonFatal
import scala.xml._

import com.vdurmont.emoji.{Emoji, EmojiParser}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document.OutputSettings.Syntax
import org.jsoup.nodes.Entities.EscapeMode
import org.jsoup.safety.{Cleaner, Whitelist}

import com.gu.kpp.nitf.{KindleHtmlToNitfConfig, XhtmlToNitfTransformer}
import com.gu.xml._


object ArticleNITF {
  val Version = "-//IPTC//DTD NITF 3.5//EN"

  private val dateFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)

  private val textCleaner = new Cleaner(Whitelist.none)

  private val htmlCleaner = new Cleaner(Whitelist.relaxed
    .addTags(KindleHtmlToNitfConfig.equivalentNitfTag.keys.toSeq: _*)
    .addProtocols("a", "href", "#")
    .removeProtocols("a", "href", "ftp", "http", "https", "mailto")
  )

  def qualify(nitf: Elem): Elem =
    nitf.copy(scope = NamespaceBinding(null, "http://iptc.org/std/NITF/2006-10-18/", TopScope))

  private def htmlToXhtml(html: String, cleaner: Cleaner = htmlCleaner): NodeSeq = {
    val saferHtml = if (html.exists(_.isControl)) html.filterNot(_.isControl) else html
    val htmlWithoutEmoji = replaceEmoji(saferHtml)  // kindle fonts don't support emojis

    val document = cleaner.clean(Jsoup.parseBodyFragment(htmlWithoutEmoji.trim))
    document.outputSettings
      .syntax(Syntax.xml)
      .escapeMode(EscapeMode.xhtml)
      .prettyPrint(false)  // actual pretty-printing will be applied to the XML


    val xhtml = document.body.outerHtml
    try {
      NodeSeq.fromSeq(XML.loadString(xhtml).child)
    } catch {
      case e: org.xml.sax.SAXParseException =>
        throw new RuntimeException(s"Failed to parse XHTML: $e\n$xhtml", e)
    }
  }

  private def replaceEmoji(source: String): String = {
    EmojiParser.parseFromUnicode(source, { unicodeCandidate =>
      val emoji = unicodeCandidate.getEmoji
      emojiCache.getOrElseUpdate(emoji, {
        val description = emoji.getDescription
        val text =
          if (description.count(_ == ' ') <= maxEmojiDescriptionWords)
            description.replace(' ', '_')
          else
            emoji.getAliases.asScala.maxBy(_.length)  // longest alias is clearer, e.g. :+1: vs :thumbs_up:
        s":$text:"
      })
    })
  }

  private val maxEmojiDescriptionWords = 3
  private val emojiCache = TrieMap.empty[Emoji, String]
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
    val pubDate = dateFormatter.format(article.pubDate)
    <head>
      <title>{article.title}</title>
      <docdata management-status="usable">
        <doc-id id-string={article.id}/>
        <urgency ed-urg="2"/>
        <date.release norm={pubDate}/>
        <doc.copyright holder="guardian.co.uk"/>
      </docdata>
      <pubdata type="print" date.publication={pubDate}/>
    </head>
  }

  private def body = transform {
    <body>
      <body.head>
        <hedline>
          <hl1>{article.title}</hl1>
        </hedline>
        <byline>{nonEmpty(article.byline.trim, ifEmpty = "–")}</byline>
        <abstract>{articleAbstract}</abstract>
      </body.head>
      <body.content>
        {mainImage ++ bodyContent}
      </body.content>
      <body.end/>
    </body>
  }

  private def articleAbstract = htmlToXhtml(article.articleAbstract)
  private def bodyContent = article.bodyBlocks.map(html => <block>{htmlToXhtml(html)}</block>)
  private def mainImage = article.mainImage.map { image =>
      <img src={image.link.source}>
        {htmlToXhtml((image.caption ++ image.credit).mkString(" "), textCleaner)}
      </img>
  }

  private def transform(elem: Elem) = {
    try {
      XhtmlToNitfTransformer(elem)
    } catch {
      case NonFatal(error) =>
        throw new TransformationException(s"Failed to generate NITF for <${elem.label}> in article ${article.id}: $error", error)
    }
  }

  private def nonEmpty(str: String, ifEmpty: String) = if (str.nonEmpty) str else ifEmpty
}
