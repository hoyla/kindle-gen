package com.gu.kindlegen

import com.gu.contentapi.client.model.v1.CapiDateTime
import com.gu.contentapi.client.model.v1._
import com.gu.contentapi.client.model.v1.ContentType
import com.gu.contentapi.client.model.v1.TagType.NewspaperBookSection
import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.client.model.v1.ContentFields
import com.gu.contentapi.client.utils.CapiModelEnrichment

/**
 * Created by alice_dee on 23/08/2017.
 */
case class TestArticle(
    testArticleNewspaperBookSection: String,
    testArticleSectionName: String,
    testArticlePageNumber: Int,
    testArticleTitle: String,
    testArticleId: String,
    testArticleIssueDate: CapiDateTime,
    // newspaperEditionDate in ContentFields
    //  releaseDate: CapiDateTime,
    //  pubDate: CapiDateTime,
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
