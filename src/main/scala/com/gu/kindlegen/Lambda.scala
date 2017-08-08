package com.gu.kindlegen

import java.io.File

import com.amazonaws.services.lambda.runtime.Context
import com.gu.contentapi.client._
import com.gu.contentapi.client.model._
import com.gu.contentapi.client.model.v1._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }
import Article._

import scala.io.Source

/**
 * This is compatible with aws' lambda JSON to POJO conversion.
 * You can test your lambda by sending it the following payload:
 * {"name": "Bob"}
 */
class LambdaInput() {
  var name: String = _
  def getName(): String = name
  def setName(theName: String): Unit = name = theName
}

case class Env(app: String, stack: String, stage: String) {
  override def toString: String = s"App: $app, Stack: $stack, Stage: $stage\n"
}

object Env {
  def apply(): Env = Env(
    Option(System.getenv("App")).getOrElse("DEV"),
    Option(System.getenv("Stack")).getOrElse("DEV"),
    Option(System.getenv("Stage")).getOrElse("DEV")
  )
}

object Lambda {

  /*
   * This is your lambda entry point
   */
  def handler(lambdaInput: LambdaInput, context: Context): Unit = {
    val logger = context.getLogger
    val env = Env()
    logger.log(s"Starting $env")
    logger.log(process(lambdaInput.name, env))
  }

  /*
   * I recommend to put your logic outside of the handler
   */
  def process(name: String, env: Env): String = s"Hello $name! (from ${env.app} in ${env.stack})\n"
}

object TestIt {
  def main(args: Array[String]): Unit = {
    println(Lambda.process(args.headOption.getOrElse("Alex"), Env()))
  }
}

object Querier {

  class PrintSentContentClient(override val apiKey: String) extends GuardianContentClient(apiKey) {
    // This overrides the val for targetUrl from "http://content.guardianapis.com"
    // the `preview.` is required in order to access the print-sent api.
    override val targetUrl = "https://preview.content.guardianapis.com/content/print-sent"
    // TODO: Find out if capi Auth or the trailing "&user-tier=internal" that is used in the kindle-previewer is required

  }
  def readApiKey = {
    // local file `~/.gu/kindle-gen.conf` must exist with first line a valid API key for CAPI.
    val localUserHome = scala.util.Properties.userHome
    val configSource = Source.fromFile(s"$localUserHome/.gu/kindle-gen.conf")
    val key = configSource.getLines.mkString
    configSource.close
    key
  }

  def printSentResponse: Seq[com.gu.contentapi.client.model.v1.Content] = {
    val capiKey = readApiKey
    val capiClient = new PrintSentContentClient(capiKey)
    val pageNum = 1
    val query = SearchQuery()
      .pageSize(1)
      .showFields("all")
    // TODO: Add error handling with Try for failed request.
    // TODO: Currently gets one result only; separate out sections and map over multiple pageSizes/ pages
    // TODO: Query requires use-date and exact date of publication
    val response = Await.result(capiClient.getResponse(query), 5.seconds)
    response.results
  }

  def resultToArticle(response: Seq[com.gu.contentapi.client.model.v1.Content]): Article = {

    val contentFields = response.map(_.fields)

    Article(
      title = response.map { content =>
        content.fields.flatMap(x => x.headline).getOrElse("")
      }.head.toString(),
      docId = response.map(_.id).head,
      issueDate = contentFields.map { f =>
        f.flatMap(x => x.newspaperEditionDate).get
      }.head,
      releaseDate = contentFields.map { f =>
        f.flatMap(x => x.newspaperEditionDate).get
      }.head,
      pubDate = contentFields.map { f =>
        f.flatMap(x => x.newspaperEditionDate).get
      }.head,
      byline = contentFields.map { f =>
        f.flatMap(x => x.byline).getOrElse("")
      }.head.toString(),
      articleAbstract = contentFields.map { f =>
        f.flatMap(x => x.standfirst).getOrElse("")
      }.head.toString(),
      content = contentFields.map { f =>
        f.flatMap(x => x.body).getOrElse("") // Note `body` used here which includes html tags. (`bodyText` strips tags)
      }.head.toString()
    )
  }
}

