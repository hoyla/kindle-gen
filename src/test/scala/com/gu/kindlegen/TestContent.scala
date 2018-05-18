package com.gu.kindlegen

import java.nio.file.{Files, Paths}
import java.time.{OffsetDateTime, ZoneOffset}

import scala.concurrent.duration._

import com.gu.contentapi.client.model.v1._
import com.gu.contentapi.client.utils.CapiModelEnrichment._
import com.gu.io.Link


object TestContent {
  val ExampleOffsetDate: OffsetDateTime = OffsetDateTime.of(2017, 7, 24, 0, 0, 0, 0, ZoneOffset.UTC)
  val ExampleDate = ExampleOffsetDate.toCapiDateTime
  val ExampleLink = Link.AbsoluteURL.from("https://www.example.com")
  lazy val ExamplePath = Link.AbsolutePath.from(Files.createDirectories(Paths.get("target", "tmp")).toRealPath())

  val ExampleQuerySettings = QuerySettings(1.minute, TagType.Type, maxImageResolution = 1000)
}

/*
 * This class is to be used as a method for creating article content of type Content for use in tests (ie unit tests that don't hit the API).
 * The case class TestContent contains all the fields we actually want/use from an article as parameters (and its easy to add some)
 * The toContent method provides Nil or None for all the (*many*) fields that the Content Type requires
 * Therefore we can pass a few parameters to TestContent and easily create a piece of Content:
 * For example:
 *
 * scala> val ta = TestContent("","",1,"","",formatter.parseDateTime("20170724").toCapiDateTime,"","","")
 * ta: com.gu.kindlegen.TestContent = TestContent(,,1,,,CapiDateTime(1500850800000,2017-07-24T00:00:00.000+01:00),,,)
 *
 * scala> ta.toContent
 * res0: com.gu.contentapi.client.model.v1.Content = Content(,Article,None,None,None,,,,Some(ContentFields(Some(),Some(),None,Some(),None,Some(),Some(1),None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,Some(CapiDateTime(1500850800000,2017-07-24T00:00:00.000+01:00)),None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None)),List(Tag(,NewspaperBook,None,None,,,,List(),None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None)),Some(List()),List(),None,None,None,None,None,None,None,None,None,false)
 *
 * More useful however is that we can use `copy` (because TestContent is a case class) to change just one of the parameters that we are interested in testing:
 *
 * scala> val ta2 = ta.copy(testArticleTitle = "new title")
 * res1: com.gu.kindlegen.TestContent = TestContent(,,1,new title,,CapiDateTime(1500850800000,2017-07-24T00:00:00.000+01:00),,,)
 *
 * scala> ta2.toContent
 * res2: com.gu.contentapi.client.model.v1.Content = Content(,Article,None,None,None,,,,Some(ContentFields(Some(new title),Some(),None,Some(),None,Some(),Some(1),None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,Some(CapiDateTime(1500850800000,2017-07-24T00:00:00.000+01:00)),None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None)),List(Tag(,NewspaperBook,None,None,,,,List(),None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None)),Some(List()),List(),None,None,None,None,None,None,None,None,None,false)
 *
*/
case class TestContent(
    testArticleNewspaperBook: String,
    testArticleSectionName: String,
    testArticlePageNumber: Int,
    testArticleTitle: String,
    testArticleId: String,
    testArticleIssueDate: CapiDateTime,
    testArticleReleaseDate: CapiDateTime,
    testArticlePubDate: CapiDateTime,
    testArticleByline: String,
    testArticleAbstract: String, // standfirst is used
    testArticleContent: String,
    testArticleImageUrl: Option[String]
) {
  import TestContent._

  def toContent: Content = Content(
    references = Nil,
    section = None,
    `type` = ContentType.Article,
    crossword = None,
    apiUrl = "",
    stats = None,
    id = testArticleId,
    tags = List(Tag(
      references = Nil,
      paidContentType = None,
      bio = None,
      paidContentCampaignColour = None,
      `type` = ExampleQuerySettings.sectionTagType,
      entityIds = None,
      emailAddress = None,
      apiUrl = "",
      id = testArticleNewspaperBook,
      tagCategories = None,
      r2ContributorId = None,
      firstName = None,
      twitterHandle = None,
      webUrl = s"https://theguardian.com/$testArticleNewspaperBook/$testArticleSectionName",
      bylineImageUrl = None,
      lastName = None,
      description = None,
      bylineLargeImageUrl = None,
      sectionName = None,
      podcast = None,
      rcsId = None,
      webTitle = testArticleSectionName,
      activeSponsorships = None,
      sectionId = None
    )),
    webUrl = s"https://theguardian.com/$testArticleId",
    fields = Option(ContentFields(
      wordcount = None,
      liveBloggingNow = None,
      newspaperEditionDate = Option(testArticleIssueDate),
      internalRevision = None,
      allowUgc = None,
      internalStoryPackageCode = None,
      commentCloseDate = None,
      standfirst = Option(testArticleAbstract),
      shortSocialShareText = None,
      sensitive = None,
      contributorBio = None,
      displayHint = None,
      shouldHideReaderRevenue = None,
      shouldHideAdverts = None,
      membershipAccess = None,
      secureThumbnail = None,
      hasStoryPackage = None,
      headline = Option(testArticleTitle),
      commentable = None,
      isPremoderated = None,
      internalComposerCode = None,
      legallySensitive = None,
      main = None,
      body = Option(testArticleContent),
      bodyText = None,
      isLive = None,
      internalPageCode = None,
      publication = None,
      trailText = None,
      isInappropriateForSponsorship = None,
      thumbnail = None,
      newspaperPageNumber = Option(testArticlePageNumber),
      creationDate = None,
      socialShareText = None,
      internalShortId = None,
      lastModified = None,
      internalVideoCode = None,
      charCount = None,
      shortUrl = None,
      internalOctopusCode = None,
      productionOffice = None,
      internalContentCode = None,
      starRating = None,
      lang = None,
      byline = Option(testArticleByline),
      firstPublicationDate = None,
      scheduledPublicationDate = None,
      showInRelatedContent = None
    )),
    isGone = None,
    atoms = None,
    sectionName = None,
    rights = None,
    webPublicationDate = None,
    debug = None,
    blocks = None,
    isHosted = false,
    webTitle = "",
    sectionId = None,
    elements = Option(Seq(element)),
    isExpired = None
  )

  def element: Element = Element(
    `type` = ElementType.Image,
    relation = "main",
    assets = Seq(asset),
    id = "",
    galleryIndex = None
  )

  def asset: Asset = Asset(
    `type` = AssetType.Image,
    typeData = Option(AssetFields(
      width = Some(500),
      secureFile = testArticleImageUrl
    ))
  )
}

