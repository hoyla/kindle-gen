package com.gu.io

import java.nio.file.Files

import scala.concurrent.{ExecutionContext, Future}

import better.files._
import org.apache.logging.log4j.scala.Logging

import com.gu.io.Link.RelativePath

case class FilePublisher(outputDirectory: File)
                        (implicit ec: ExecutionContext) extends Publisher with Logging {
  private val dir = outputDirectory.createDirectories()
  private val dirLink = Link.AbsolutePath.from(dir.path)

  override type PublishedLink = RelativePath

  override def persist(content: Array[Byte], fileName: String): Future[PublishedLink] = Future {
    val file = resolve(fileName)
    logger.debug(s"Writing $fileName to $file")

    file.writeByteArray(content)
    logger.info(s"Wrote file to $file")

    toLink(file)
  }

  def zipPublications(archiveName: String = "archive.zip"): Future[PublishedLink] = Future {
    val files = publications.map(x => File(x.toPath)).toSeq

    logger.debug(s"Compressing ${files.size} files into $archiveName")
    val archive = resolve(archiveName).zipIn(files.iterator)()

    toLink(archive)
  }

  def delete(fileName: String): Future[Unit] = {
    val path = resolve(fileName).path
    logger.debug(s"Deleting $path...")

    savedLinks.remove(toLink(path))
    Future {
      val deleted = Files.deleteIfExists(path)

      logger.info(s"Deleted $path")
      if (!deleted)
        logger.debug(s"Deleting $path: file could not be deleted because it did not exist.")
    }
  }

  private def resolve(fileName: String) = {
    require(!fileName.contains(java.io.File.separatorChar), s"Invalid file name! $fileName")
    dir / fileName
  }

  private def toLink(file: File): PublishedLink = {
    RelativePath.from(dir.relativize(file), relativeTo = dirLink)
  }
}
