package com.gu.io.aws

import java.nio.file.Path

import scala.concurrent.{ExecutionContext, Future}

import com.amazonaws.services.s3.AmazonS3

import com.gu.io.{FilePublisher, Link, Publisher}
import com.gu.io.Link.{AbsolutePath, RelativePath}

trait S3Settings {
  def bucketName: String
  def bucketDirectory: String
  def tmpDirOnDisk: Path

  val absolutePath: AbsolutePath = AbsolutePath.from(s"/$bucketName/$bucketDirectory")
}

case class S3Publisher(s3: AmazonS3, settings: S3Settings)
                      (implicit ec: ExecutionContext) extends Publisher {
  import settings._

  private val tmpPublisher = FilePublisher(tmpDirOnDisk.resolve(bucketDirectory))

  override def persist(content: Array[Byte], fileName: String): Future[Link] = {
    // write the file to disk temporarily; the S3 client API for files infers the metadata automatically
    tmpPublisher.persist(content, fileName).map { relativePath =>
      s3.putObject(bucketName, s"$bucketDirectory/$fileName", relativePath.toPath.toFile)
      RelativePath.from(fileName, relativeTo = settings.absolutePath)
    }.andThen { case _ =>
      tmpPublisher.delete(fileName)
    }
  }

  override def publish(): Future[Unit] = {
    // TODO mark `bucketDirectory` for publishing
    super.publish()
  }
}
