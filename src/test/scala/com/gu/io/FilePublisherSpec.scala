package com.gu.io

import java.nio.file.{Files, Path, Paths}

import scala.concurrent.ExecutionContext

import org.scalatest.FunSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import com.gu.concurrent.TestExecutionContext


class FilePublisherSpec extends FunSpec with TempFiles {
  private implicit val ec: ExecutionContext = TestExecutionContext

  private val tempDir = newTempDir(Paths.get("target", "tmp"))
  private val publisher = FilePublisher(tempDir)

  describe(".save") {
    it("writes content to disk") {
      val bytes = "Hello".getBytes()

      val saved = save("hello.txt", bytes)
      Files.readAllBytes(saved) shouldBe bytes
    }

    it("tracks saved files") {
      val saved = save("empty.txt")
      publisher.publications.map(_.toPath.toRealPath()) should contain(saved)
    }
  }

  describe(".delete") {
    it("deletes the file") {
      val saved = save("empty.txt")
      publisher.delete("empty.txt").futureValue
      Files.exists(saved) shouldBe false
    }

    it("is graceful with non-existing files") {
      publisher.delete("non-existing").futureValue  // must not throw an exception
    }
  }

  describe(".publish") {
    it("leaves published files as-is (i.e. doesn't delete them)") {
      val saved = save("empty.txt")
      publisher.publish().futureValue
      Files.exists(saved) shouldBe true
    }
  }

  private def save(fileName: String, content: Array[Byte] = Array.emptyByteArray): Path = {
    publisher.save(content, fileName)
      .map { link =>
        val path = link.toPath.toRealPath()
        trackTempFile(path)
          .ensuring(_ == tempDir.resolve(fileName).toRealPath())
      }.futureValue
  }
}
