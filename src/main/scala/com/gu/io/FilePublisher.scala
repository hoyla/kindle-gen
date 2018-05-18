package com.gu.io

import java.io.File
import java.nio.file.{Files, Path}

import scala.concurrent.{ExecutionContext, Future}

import com.gu.io.Link.RelativePath

case class FilePublisher(outputDirectory: Path)
                        (implicit ec: ExecutionContext) extends Publisher {
  private val dir = Files.createDirectories(outputDirectory).toRealPath()
  private val dirLink = Link.AbsolutePath.from(dir)

  override def persist(content: Array[Byte], fileName: String): Future[RelativePath] = Future {
    val path = Files.write(toPath(fileName), content)
    toLink(path)
  }

  def delete(fileName: String): Future[Unit] = {
    val path = toPath(fileName)
    savedLinks.remove(toLink(path))
    Future { Files.deleteIfExists(path) }
  }

  private def toPath(fileName: String) = {
    require(!fileName.contains(File.separatorChar), s"Invalid file name! $fileName")
    dir.resolve(fileName)
  }

  private def toLink(path: Path) = {
    RelativePath.from(dir.relativize(path), relativeTo = dirLink)
  }
}
