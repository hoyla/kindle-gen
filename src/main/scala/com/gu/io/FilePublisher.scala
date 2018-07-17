package com.gu.io

import java.nio.file.{Files, Path}

import scala.concurrent.{ExecutionContext, Future}

import org.apache.logging.log4j.scala.Logging

import com.gu.io.Link.RelativePath

case class FilePublisher(outputDirectory: Path)
                        (implicit ec: ExecutionContext) extends Publisher with Logging {
  private val dir = Files.createDirectories(outputDirectory).toRealPath()
  private val dirLink = Link.AbsolutePath.from(dir)

  override type PublishedLink = RelativePath

  override def persist(content: Array[Byte], fileName: String): Future[PublishedLink] = Future {
    val path = toPath(fileName)
    logger.debug(s"Writing $fileName to $path")

    Files.write(path, content)
    logger.info(s"Wrote file to $path")

    toLink(path)
  }

  def zipPublications(archiveName: String = "archive.zip"): Future[PublishedLink] = Future {
    import better.files._
    val files = publications.map(x => File(x.toPath)).toSeq

    logger.debug(s"Compressing ${files.size} files into $archiveName")
    val archive = (File(dir) / archiveName).zipIn(files.iterator)()

    toLink(archive.path)
  }

  def delete(fileName: String): Future[Unit] = {
    val path = toPath(fileName)
    logger.debug(s"Deleting $path...")

    savedLinks.remove(toLink(path))
    Future {
      val deleted = Files.deleteIfExists(path)

      logger.info(s"Deleted $path")
      if (!deleted)
        logger.debug(s"Deleting $path: file could not be deleted because it did not exist.")
    }
  }

  private def toPath(fileName: String) = {
    require(!fileName.contains(java.io.File.separatorChar), s"Invalid file name! $fileName")
    dir.resolve(fileName)
  }

  private def toLink(path: Path) = {
    RelativePath.from(dir.relativize(path), relativeTo = dirLink)
  }
}
