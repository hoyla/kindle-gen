package com.gu.kindlegen

import com.gu.contentapi.client.model.v1.CapiDateTime
import com.gu.contentapi.client.model.v1._
import com.gu.contentapi.client.model.v1.ContentType
import com.gu.contentapi.client.model.v1.TagType.NewspaperBookSection
import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.client.model.v1.ContentFields
import com.gu.contentapi.client.utils.CapiModelEnrichment

/*
*
 * This class is to be used as a method for creating article content of type Content for use in tests (ie unit tests that don't hit the API).
 * The case class TestArticle contains all the fields we actually want/use from an article as parameters (and its easy to add some)
 * The toContent method provides Nil or None for all the (*many*) fields that the Content Type requires
 * Therefore we can pass a few parameters to TestArticle and easily create a piece of Content:
 * For example:
 *
 * scala> val ta = TestArticle("","",1,"","",CapiModelEnrichment.RichJodaDateTime(formatter.parseDateTime("20170724")).toCapiDateTime,"","","")
 * ta: com.gu.kindlegen.TestArticle = TestArticle(,,1,,,CapiDateTime(1500850800000,2017-07-24T00:00:00.000+01:00),,,)
 *
 * scala> ta.toContent
 * res0: com.gu.contentapi.client.model.v1.Content = Content(,Article,None,None,None,,,,Some(ContentFields(Some(),Some(),None,Some(),None,Some(),Some(1),None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,Some(CapiDateTime(1500850800000,2017-07-24T00:00:00.000+01:00)),None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None)),List(Tag(,NewspaperBookSection,None,None,,,,List(),None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None)),Some(List()),List(),None,None,None,None,None,None,None,None,None,false)
 *
 * More useful however is that we can use `copy` (because TestArticle is a case class) to change just one of the parameters that we are interested in testing:
 *
 * scala> val ta2 = ta.copy(testArticleTitle = "new title")
 * res1: com.gu.kindlegen.TestArticle = TestArticle(,,1,new title,,CapiDateTime(1500850800000,2017-07-24T00:00:00.000+01:00),,,)
 *
 * scala> ta2.toContent
 * res2: com.gu.contentapi.client.model.v1.Content = Content(,Article,None,None,None,,,,Some(ContentFields(Some(new title),Some(),None,Some(),None,Some(),Some(1),None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,Some(CapiDateTime(1500850800000,2017-07-24T00:00:00.000+01:00)),None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None)),List(Tag(,NewspaperBookSection,None,None,,,,List(),None,None,None,None,None,None,None,None,None,None,None,None,None,None,None,None)),Some(List()),List(),None,None,None,None,None,None,None,None,None,false)
 *
*/
case class TestArticle(
    testArticleNewspaperBookSection: String,
    testArticleSectionName: String,
    testArticlePageNumber: Int,
    testArticleTitle: String,
    testArticleId: String,
    testArticleIssueDate: CapiDateTime,
    // TODO: for clarity add pub-and release- dates or remove from other Article methods
    testArticleByline: String,
    testArticleAbstract: String, // standfirst is used
    testArticleContent: String
) {
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
      `type` = NewspaperBookSection,
      entityIds = None,
      emailAddress = None,
      apiUrl = "",
      id = testArticleNewspaperBookSection,
      tagCategories = None,
      r2ContributorId = None,
      firstName = None,
      twitterHandle = None,
      webUrl = "",
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
    webUrl = "",
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
    elements = Option(Nil),
    isExpired = None
  )
}
