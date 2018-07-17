package com.gu.io

import java.nio.file.{Files, Path}

import scala.collection.immutable.ListSet
import scala.util.Try

import better.files._
import org.scalatest.{BeforeAndAfterAll, Suite}


trait TempFiles extends BeforeAndAfterAll { this: Suite =>
  var createdTempPaths = ListSet.empty[File]
  def trackTempFile(x: File): Path = { createdTempPaths += x; x.path }

  protected def newTempDir: File = trackTempFile(File.newTemporaryDirectory())
  protected def newTempFile: File = trackTempFile(File.newTemporaryFile())
  protected def newTempDir(parent: File): File = trackTempFile(File.newTemporaryDirectory(parent = Some(parent)))
  protected def newTempFile(parent: File): File = trackTempFile(File.newTemporaryFile(parent = Some(parent)))

  protected override def afterAll(): Unit =
    createdTempPaths.toList.reverse.foreach(file => Try(Files.deleteIfExists(file.path)))
}
