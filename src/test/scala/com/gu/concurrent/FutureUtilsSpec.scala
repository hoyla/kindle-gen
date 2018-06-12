package com.gu.concurrent

import scala.concurrent.Future
import scala.util.Random

import org.scalatest.FunSpec
import org.scalatest.Matchers._

import com.gu.concurrent.FutureUtils._
import org.scalatest.concurrent.ScalaFutures._

class FutureUtilsSpec extends FunSpec {
  implicit val ec = SideEffectsExecutionContext

  describe("successfulSequence") {
    it("returns a successful future when given successful futures") {
      val values = Seq(1, 2, 3)
      val futures = values.map(Future.successful)
      successfulSequence(futures).futureValue shouldBe values
    }

    it("returns a successful future when given failing futures") {
      val futures = (1 to 3).map(_ => Future.failed(new RuntimeException))
      successfulSequence(futures).futureValue shouldBe empty
    }

    it("returns a successful future when given mixed futures") {
      val values = Seq(1, 2, 3)
      val successes = values.map(Future.successful)
      val failures = (1 to 3).map(_ => Future.failed(new RuntimeException))

      val futures = Random.shuffle(successes ++ failures)
      successfulSequence(futures).futureValue should contain theSameElementsAs values
    }
  }
}
