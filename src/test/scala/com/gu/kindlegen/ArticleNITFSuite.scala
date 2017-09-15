package com.gu.kindlegen

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.gu.contentapi.client.utils._
import DateUtils._

@RunWith(classOf[JUnitRunner])
class ArticleNITFSuite extends FunSuite {

  test("ArticleNITF apply") {
    val article = Article(
      newspaperBookSection = "theguardian/mainsection/international",
      sectionName = "International",
      newspaperPageNumber = 2,
      title = "my title",
      docId = "section/date/title",
      issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
      releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
      pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
      byline = "my name",
      articleAbstract = "article abstract",
      content = "content",
      imageUrl = None,
      fileId = 0
    )

    val expectedOutput =
      """
        |<?xml version="1.0" encoding="UTF-8"?>
        |<nitf version="-//IPTC//DTD NITF 3.3//EN">
        |<head>
        |<title>my title</title>
        |<docdata management-status="usable">
        |<doc-id id-string="section/date/title" />
        |<urgency ed-urg="2" />
        |<date.issue norm="20170724" />
        |<date.release norm="20170724" />
        |<doc.copyright holder="guardian.co.uk" />
        |</docdata>
        |<pubdata type="print" date.publication="20170724" />
        |</head>
        |<body>
        |<body.head>
        |<hedline><hl1>my title</hl1></hedline>
        |<byline>my name</byline>
        |<abstract>article abstract</abstract>
        |</body.head>
        |<body.content>content</body.content>
        |<body.end />
        |</body>
        |</nitf>""".stripMargin
    val aNitf = ArticleNITF(article)
    assert(aNitf.fileContents === expectedOutput)
  }

  test("filterBodyContent") {
    val exampleContent =
      """
        |<p>To call Colossal tonally uneven would perhaps be missing the entire point of Colossal. For months now, the staggeringly odd premise has been the source of feverish online discussion and intense confusion. She did what? And has a what? But how could that? The answers are here and, well, they’re far from befitting of that title ...</p><p>Anne Hathaway plays Gloria, a woman stuck in a rut of her own making, drinking too much and eventually causing her boyfriend to end their relationship, frustrated by her childish excess. Gloria is wrecked and decides to move from the big city back to her hometown, where she reconnects with childhood friend Oscar (Jason Sudeikis), taking a job in his bar. So far so Anne Hathaway movie. </p>
        |<p>But as Gloria starts to settle into her new lifestyle, something terrible happens. The city of Seoul is invaded by a giant monster and, despite living on the other side of the world, Gloria feels a deep connection to the devastating events. But this isn’t just the case of a bleeding heart, this is an out of body experience as Gloria finds that she is somehow strangely tied to the creature and the fate of the world just might rest on her unlikely shoulders.</p>
        |<p>On paper, there’s quite literally nothing like Colossal. Writer/director Nacho Vigalondo deserves immediate plaudits for crafting a premise that reads like Rachel Getting Married meets Godzilla (in fact, the film’s resemblance to the latter <a href="https://www.theguardian.com/film/2015/may/20/godzilla-toho-sues-over-anne-hathaway-rival-movie-colossal">led to a lawsuit</a>) and the leftfield genre shift comes as a refreshing change, after another particularly dull set of summer blockbusters. But, given the bizarro conceit, there’s something surprisingly, and frustratingly, safe about the film.</p>
        |<p>Admittedly, in the outset this is rather deliberate. Vigalondo shoots it like a charming Fox Searchlight indie about a woman rediscovering herself and while it’s fun to see this cosey first act make a hard left into b-movie territory, it’s also difficult not to find the characters and interplay just as cloying and familiar as they are in the film it’s initially pretending to ape. But there doesn’t seem to be much of a sense of irony and, instead, we’re expected to invest in these paper-thin stock indie movie types, finding their cutesy conversations to be relatable.</p><p>When the monster mash-up eventually ensues, it’s a welcome relief and provides an unusual spin on a familiar tale but there remains a nagging feeling that none of this is quite as sharp as it should be. Jokes fall flat, comedy is overly broad and nuance is at a bare minimum. As Gloria delves deeper into the reason why she’s connected to a giant monster destroying South Korea, there are interesting ideas about self-importance during times of internal struggle and the gap between those who moved away and those who stayed in their hometown. But they’re half-explored and ultimately rejected for an unlikely and underdeveloped rivalry that leads to the appearance of a giant robot ...</p>
        |<p>Hathaway remains an engaging presence as ever but Gloria’s journey is overly, boringly simplified with her alcoholism and personal issues given a glossy Nancy Meyers makeover. It remains a surprise throughout that the film feels like such studio product, given the potentially edgy small-scale drama it could have been. It does result in the giant creature looking blockbuster-worthy but it leaves the edges feeling smoothed out.</p>
        |<p>Colossal remains something of a fascinating misfire and quite easily the hardest film to market this year. It’s a tantalising missed opportunity because if you’re going to make a film about a woman with a psychic connection to a giant city-crushing monster, it should definitely be weirder than this. </p>
        |
    """.stripMargin

    val exampleFilteredContent =
      """
        |<p>To call Colossal tonally uneven would perhaps be missing the entire point of Colossal. For months now, the staggeringly odd premise has been the source of feverish online discussion and intense confusion. She did what? And has a what? But how could that? The answers are here and, well, they're far from befitting of that title ...</p><p>Anne Hathaway plays Gloria, a woman stuck in a rut of her own making, drinking too much and eventually causing her boyfriend to end their relationship, frustrated by her childish excess. Gloria is wrecked and decides to move from the big city back to her hometown, where she reconnects with childhood friend Oscar (Jason Sudeikis), taking a job in his bar. So far so Anne Hathaway movie. </p>
        |<p>But as Gloria starts to settle into her new lifestyle, something terrible happens. The city of Seoul is invaded by a giant monster and, despite living on the other side of the world, Gloria feels a deep connection to the devastating events. But this isn't just the case of a bleeding heart, this is an out of body experience as Gloria finds that she is somehow strangely tied to the creature and the fate of the world just might rest on her unlikely shoulders.</p>
        |<p>On paper, there's quite literally nothing like Colossal. Writer/director Nacho Vigalondo deserves immediate plaudits for crafting a premise that reads like Rachel Getting Married meets Godzilla (in fact, the film's resemblance to the latter led to a lawsuit) and the leftfield genre shift comes as a refreshing change, after another particularly dull set of summer blockbusters. But, given the bizarro conceit, there's something surprisingly, and frustratingly, safe about the film.</p>
        |<p>Admittedly, in the outset this is rather deliberate. Vigalondo shoots it like a charming Fox Searchlight indie about a woman rediscovering herself and while it's fun to see this cosey first act make a hard left into b-movie territory, it's also difficult not to find the characters and interplay just as cloying and familiar as they are in the film it's initially pretending to ape. But there doesn't seem to be much of a sense of irony and, instead, we're expected to invest in these paper-thin stock indie movie types, finding their cutesy conversations to be relatable.</p><p>When the monster mash-up eventually ensues, it's a welcome relief and provides an unusual spin on a familiar tale but there remains a nagging feeling that none of this is quite as sharp as it should be. Jokes fall flat, comedy is overly broad and nuance is at a bare minimum. As Gloria delves deeper into the reason why she's connected to a giant monster destroying South Korea, there are interesting ideas about self-importance during times of internal struggle and the gap between those who moved away and those who stayed in their hometown. But they're half-explored and ultimately rejected for an unlikely and underdeveloped rivalry that leads to the appearance of a giant robot ...</p>
        |<p>Hathaway remains an engaging presence as ever but Gloria's journey is overly, boringly simplified with her alcoholism and personal issues given a glossy Nancy Meyers makeover. It remains a surprise throughout that the film feels like such studio product, given the potentially edgy small-scale drama it could have been. It does result in the giant creature looking blockbuster-worthy but it leaves the edges feeling smoothed out.</p> <p>Colossal remains something of a fascinating misfire and quite easily the hardest film to market this year. It's a tantalising missed opportunity because if you're going to make a film about a woman with a psychic connection to a giant city-crushing monster, it should definitely be weirder than this. </p>
        |
    """.stripMargin

    val article2 = Article(
      newspaperBookSection = "theguardian/mainsection/international",
      sectionName = "International",
      newspaperPageNumber = 2,
      title = "my title",
      docId = "section/date/title",
      issueDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
      releaseDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
      pubDate = CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,
      byline = "my name",
      articleAbstract = "article abstract",
      content = exampleContent,
      imageUrl = None,
      fileId = 0
    )

    val expectedOutput2 =
      s"""
         |<?xml version="1.0" encoding="UTF-8"?>
         |<nitf version="-//IPTC//DTD NITF 3.3//EN">
         |<head>
         |<title>my title</title>
         |<docdata management-status="usable">
         |<doc-id id-string="section/date/title" />
         |<urgency ed-urg="2" />
         |<date.issue norm="20170724" />
         |<date.release norm="20170724" />
         |<doc.copyright holder="guardian.co.uk" />
         |</docdata>
         |<pubdata type="print" date.publication="20170724" />
         |</head>
         |<body>
         |<body.head>
         |<hedline><hl1>my title</hl1></hedline>
         |<byline>my name</byline>
         |<abstract>article abstract</abstract>
         |</body.head>
         |<body.content>${exampleFilteredContent}</body.content>
         |<body.end />
         |</body>
         |</nitf>""".stripMargin

    assert(ArticleNITF.filterBodyContent(article2) === exampleFilteredContent)
  }

}
