package com.gu.kindlegen
import java.awt.image.BufferedImage
import java.io._
import javax.imageio.ImageIO

import com.gu.contentapi.client._
import com.gu.contentapi.client.model._
import com.gu.contentapi.client.model.v1.Content

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.{ BufferedSource, Source }
import scala.concurrent.ExecutionContext.Implicits.global
import scalaj.http._
import DateUtils._
import com.gu.contentapi.client.model.v1.TagType.NewspaperBookSection

class Querier {
}

object Querier {

  def readConfig(lineNum: Int): String = {
    // local file `~/.gu/kindle-gen.conf` must exist with first line a valid API key for CAPI. Second line targetUrl
    val localUserHome: String = scala.util.Properties.userHome
    val configSource: BufferedSource = Source.fromFile(s"$localUserHome/.gu/kindle-gen.conf")
    val arr = configSource.getLines.toArray
    arr(lineNum)
  }

  class PrintSentContentClient(override val apiKey: String) extends GuardianContentClient(apiKey) {

    override val targetUrl: String = readConfig(1)
  }

  val readApiKey: String = readConfig(0)

  def getPrintSentResponse: Seq[Content] = {

    val capiKey = readApiKey
    val capiClient = new PrintSentContentClient(capiKey)
    val pageNum = 1
    val query = SearchQuery()
      .pageSize(5)
      .showFields("all")
      .orderBy("newest")
      .fromDate(editionDateStart)
      .toDate(editionDateEnd)
      .useDate("newspaper-edition")
      //      .page(pageNum)
      .showFields("newspaper-page-number, headline,newspaper-edition-date,byline,standfirst,body")
      // TODO: what fields are required? main? content? Is tag `newspaper-book` required
      .showTags("newspaper-book-section, newspaper-book")
      .showElements("image")
    // TODO: Add error handling with Try for failed request.
    // TODO: Currently gets one result only; separate out sections and map over multiple pageSizes/ pages
    // TODO: Query requires use-date and exact date of publication
    val response = Await.result(capiClient.getResponse(query), 5.seconds)
    response.results
  }

  // TODO: handle the possibility of there being no content in the getPrintSentResponse method above
  /*  TODO: 1. this should possibly be a map with the associated article id/number so that they can be paired up later
*/
  /* TODO:
      - sort content by page number, and then booksection
      because a page CAN have more than one booksection on it.
      - for each with index, create article and put index into article as
      identifier
      - add identifier to article as field ( the docId is a string - can I use the internalPageCode? Is it unique? Ask DB)
      -
  */

  def responseToArticles(response: Seq[Content]): Seq[Article] = {
    val sortedContent = Querier.sortContentByPageAndSection(response)
    val contentWithIndex = sortedContent.view.zipWithIndex.toList
    contentWithIndex.map(Article.apply)
  }
  // Probably best to merge these functions OR
  // put the article index/id into the article class so you dont have to pass around a tuple.
  //  def articlesWithFileIdentifier(articles: Seq[Article]): List[(Article, Int)] = {
  //    articles.view.zipWithIndex.toList
  //  }

  def sortContentByPageAndSection(response: Seq[Content]): Seq[Content] = {
    response.sortBy(content => (content.fields.flatMap(_.newspaperPageNumber), content.tags.find(_.`type` == NewspaperBookSection).get.id))
  }

  // TODO: To many IO methods make up this one - try to reduce to one untestable method with the rest being pure
  // TODO: Test: Will this catch exception if filename exists?
  def fetchAndWriteImages(articlesIDs: List[(Article, Int)]): Unit = {
    articlesIDs.foreach {
      case (article, i) => {
        val urlOption = article.imageUrl
        val dataOption = urlOption.flatMap(getImageData)
        val bufferedOption = imageBytesToBuffered(dataOption)
        writeImageToFile((bufferedOption, i))
      }
    }
  }

  // TODO: Should this be a Map or list of KV pairs (tuples)?
  // FIXME: this is slow... why?
  def imageUrlsWithIDs(articlesIDs: List[(Article, Int)]): List[(Option[String], Int)] = {
    articlesIDs.map {
      case (article, i) => (article.imageUrl, i)
    }
  }

  // look up try monad instead of try catch
  // This might fail so an option is returned
  def getImageData(url: String): Option[Array[Byte]] =
    try {
      val response: HttpRequest = Http(url)
      Some(response.asBytes.body)
    } catch {
      case e: Exception => None
    }

  // dont need this method or the write to file. Can send the byte array to the ftp server
  // Remove IO read and writes methods, and store bytearray in memory. Then use the Amazon ftp server api to create the files on their server - cuts down on processing time as read and write are resource-heavy ops.
  // would be a good idea to model the File structure
  def imageBytesToBuffered(imageData: Option[Array[Byte]]): Option[BufferedImage] = {
    try {
      val inputStreamBytes: InputStream = new ByteArrayInputStream(imageData.get)
      val bufferedImageOut: BufferedImage = ImageIO.read(inputStreamBytes)
      Some(bufferedImageOut)
    } catch {
      case e: Exception => None
    }
  }

  // TODO: write unit test for sad path
  // TODO: will require an integration test
  // This should just take a byte array and a filename and write the bytearray to the filename
  // shouldnt have the word image in
  // can go in a different object
  // But I don't need to create these files yet - but it might be a good idea to model it with an ftp file
  // class which has a file structure and file names and a file path and a place for the data etc
  // Can delete this method now I know it works and I get an image.
  def writeImageToFile(imageWithId: (Option[BufferedImage], Int)): Unit = {
    try {
      val image = imageWithId._1.get
      val id = imageWithId._2
      // TODO: Folder/path to write file required
      ImageIO.write(image, "jpg", new File(s"image${id}_500.jpg"))
      println(s"writing image file for article $id")
    } catch {
      case e: Exception => println(s"No suitable image found for article${imageWithId._2}")
    }
  }
}