package com.gu.io

import java.nio.file.{Files, Path}

import scala.collection.immutable.ListSet
import scala.util.Try

import org.scalatest.{BeforeAndAfterAll, Suite}


trait TempFiles extends BeforeAndAfterAll { this: Suite =>
  import Files._

  protected var createdTempPaths: ListSet[Path] = ListSet.empty
  protected def trackTempFile(x: Path): Path = { createdTempPaths += x; x }

  protected def newTempDir: Path = trackTempFile(createTempDirectory(null))
  protected def newTempFile: Path = trackTempFile(createTempFile(null, null))
  protected def newTempDir(parent: Path): Path = trackTempFile(createTempDirectory(parent, null))
  protected def newTempFile(parent: Path): Path = trackTempFile(createTempFile(parent, null, null))

  protected override def afterAll(): Unit =
    createdTempPaths.iterator.toList.reverse.foreach(path => Try(Files.deleteIfExists(path)))
}
