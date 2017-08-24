package com.gu.kindlegen

//import com.github.nscala_time.time.Imports._
import com.gu.contentapi.client.model.v1.TagType.NewspaperBookSection
import com.gu.contentapi.client.model.v1._
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

case class Article(
  newspaperBookSection: String,
  sectionName: String,
  newspaperPageNumber: Int,
  title: String,
  docId: String,
  issueDate: CapiDateTime,
  // newspaperEditionDate in ContentFields
  releaseDate: CapiDateTime,
  pubDate: CapiDateTime,
  byline: String,
  articleAbstract: String, // standfirst is used
  content: String
)

object Article {
  def apply(content: Content) = new Article(
    newspaperBookSection = content.tags.find(_.`type` == NewspaperBookSection).get.id, // FIXME: NB this will throw exception if this tag is missing!
    sectionName = content.tags.find(_.`type` == NewspaperBookSection).get.webTitle, // FIXME: NB this will throw exception if this tag is missing!
    newspaperPageNumber = content.fields.flatMap(_.newspaperPageNumber).getOrElse(0),
    title = content.fields.flatMap(_.headline).getOrElse("").toString,
    docId = content.id,
    issueDate = content.fields.flatMap(_.newspaperEditionDate).get,
    releaseDate = content.fields.flatMap(_.newspaperEditionDate).get,
    pubDate = content.fields.flatMap(_.newspaperEditionDate).get,
    byline = content.fields.flatMap(_.byline).getOrElse(""),
    articleAbstract = content.fields.flatMap(_.standfirst).getOrElse(""),
    content = content.fields.flatMap(_.body).getOrElse("")
  )
}