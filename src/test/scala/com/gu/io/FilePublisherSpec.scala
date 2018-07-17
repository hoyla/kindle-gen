package com.gu.io

import scala.collection.mutable
import scala.concurrent.ExecutionContext

import better.files._
import org.scalatest.FunSpec
import org.scalatest.Inspectors._
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import com.gu.concurrent.TestExecutionContext


class FilePublisherSpec extends FunSpec with TempFiles {
  private implicit val ec: ExecutionContext = TestExecutionContext

  private val tempDir = newTempDir("target" / "tmp")
  private val publisher = FilePublisher(tempDir)

  describe(".save") {
    it("writes content to disk") {
      val bytes = "Hello".getBytes()

      val saved = save("hello.txt", bytes)
      saved.byteArray shouldBe bytes
    }

    it("tracks saved files") {
      val saved = (1 to 3).map { i => save(s"empty$i.txt").path }
      publisher.publications.map(_.toPath.toRealPath()) should contain allElementsOf saved
    }
  }

  describe(".delete") {
    it("is graceful with non-existing files") {
      publisher.delete("non-existing").futureValue  // must not throw an exception
    }

    it("deletes the file") {
      val saved = save("empty.txt")
      publisher.delete("empty.txt").futureValue
      saved.exists() shouldBe false
    }
  }

  describe(".publish") {
    it("leaves published files as-is (i.e. doesn't delete them)") {
      val saved = save("empty.txt")
      publisher.publish().futureValue
      saved.exists() shouldBe true
    }
  }

  describe(".zipPublications") {
    it("saves a zip file with all published contents") {
      val entries = Set("Hi.txt", "Hello.bin", "Ahlan").map { x =>
        val bytes = x.getBytes
        save(x, bytes)
        x -> bytes
      }.toMap

      val zipped: File = publisher.zipPublications("my.zip").futureValue.toPath
      trackTempFile(zipped)

      val zippedFileSizes = mutable.Map.empty[String, Long]
      zipped.unzipTo("/dev"/"null", { zipEntry =>
        zippedFileSizes.put(zipEntry.getName, zipEntry.getSize)
        false  // no actual extraction
      })

      zippedFileSizes should not contain key("my.zip")

      forAll(entries) { case (name, bytes) =>
        zippedFileSizes should contain key name
        zippedFileSizes(name) shouldBe bytes.length
      }
    }
  }

  private def save(fileName: String, content: Array[Byte] = Array.emptyByteArray): File = {
    publisher.save(content, fileName)
      .map { link =>
        val path = link.toPath.toRealPath()
        trackTempFile(path)
          .ensuring(_ == (tempDir / fileName).path.toRealPath())
      }.futureValue
  }
}
