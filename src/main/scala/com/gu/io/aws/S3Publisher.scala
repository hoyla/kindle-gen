package com.gu.io.aws

import java.util
import java.nio.file.Path

import scala.collection.JavaConverters._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{BucketWebsiteConfiguration, RedirectRule, RoutingRule, RoutingRuleCondition}
import org.apache.logging.log4j.scala.Logging

import com.gu.io.{FilePublisher, Link, Publisher}
import com.gu.io.Link.{AbsolutePath, RelativePath}


trait S3PublisherSettings {
  def bucketName: String
  def bucketDirectory: String
  def tmpDirOnDisk: Path

  val absolutePath: AbsolutePath = AbsolutePath.from(s"/$bucketName/$bucketDirectory")
}


case class S3Publisher(s3: AmazonS3, settings: S3PublisherSettings)
                      (implicit ec: ExecutionContext) extends Publisher with Logging {
  override type PublishedLink = RelativePath

  logger.trace(s"Initialised with $settings")
  import settings._

  private val tmpPublisher = FilePublisher(tmpDirOnDisk.resolve(bucketDirectory))

  private lazy val websiteConfig =
    Option(s3.getBucketWebsiteConfiguration(bucketName)).getOrElse(new BucketWebsiteConfiguration())

  private lazy val routingRules =
    Option(websiteConfig.getRoutingRules).getOrElse(new util.ArrayList[RoutingRule](1))

  override def persist(content: Array[Byte], fileName: String): Future[PublishedLink] = {
    // write the file to disk temporarily; the S3 client API infers the metadata from files automatically
    tmpPublisher.persist(content, fileName).map { relativePath =>
      val s3Path = normalizeDir(bucketDirectory) + fileName

      logger.debug(s"Uploading $fileName to $s3Path...")
      s3.putObject(bucketName, s3Path, relativePath.toPath.toFile)
      logger.info(s"Uploaded file to $s3Path")

      RelativePath.from(fileName, relativeTo = settings.absolutePath)
    }
  }

  override def close(): Unit = {
    val deleteAll = tmpPublisher.publications.map(link => tmpPublisher.delete(link.source))
    Await.ready(Future.sequence(deleteAll), 1.minute)  // ignore errors
    tmpPublisher.close()
    super.close()
  }

  /** Redirects publicDirectory to settings.bucketDirectory */
  def redirect(publicDirectory: String) = Future {
    removeRedirect(publicDirectory)
    routingRules.add(redirectRule(publicDirectory, bucketDirectory))
    publishRedirects()
  }

  /** Removed the redirect from publicDirectory, if any */
  def undirect(publicDirectory: String) = Future {
    if (removeRedirect(publicDirectory))
      publishRedirects()
  }

  private def removeRedirect(publicDirectory: String): Boolean = {
    val publicDir = normalizeDir(publicDirectory)
    routingRules.removeIf(publicDir == _.getCondition.getKeyPrefixEquals)
  }

  private def publishRedirects() = {
    websiteConfig.setRoutingRules(routingRules)
    s3.setBucketWebsiteConfiguration(bucketName, websiteConfig)
  }

  private def redirectRule(fromDirectory: String, toDirectory: String) = {
    val fromDir = normalizeDir(fromDirectory)
    val toDir = normalizeDir(toDirectory)
    new RoutingRule()
      .withCondition(new RoutingRuleCondition().withKeyPrefixEquals(fromDir))
      .withRedirect(new RedirectRule().withReplaceKeyPrefixWith(toDir))
  }

  private def normalizeDir(dirName: String): String = {
    if (dirName.endsWith("/")) dirName else s"$dirName/"
  }
}
