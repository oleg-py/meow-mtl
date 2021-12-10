package com.olegpy.meow.effects

import cats.effect.IO
import cats.effect.kernel.Ref
import cats.effect.testkit.TestContext
import cats.effect.testkit.TestInstances
import cats.implicits._
import cats.mtl.laws.discipline._
import minitest.SimpleTestSuite
import minitest.laws.Checkers
import org.typelevel.discipline.Laws

import scala.concurrent.duration._

object EffectInstancesLawsSuite extends SimpleTestSuite with Checkers with TestInstances {

  private def checkAll(name: String)(ruleSet: Ticker => Laws#RuleSet) = {
    implicit val ticker = Ticker(TestContext())

    for ((id, prop) <- ruleSet(ticker).all.properties)
      test(name + "." + id) {
        ticker.ctx.advanceAndTick(1.day)
        check(prop)
      }
  }

  checkAll("Ref.runAsk") { implicit ticker =>
    Ref.unsafe[IO, Int](0).runAsk(ev =>
      AskTests(ev).ask[Int]
    )
  }

  checkAll("Ref.runState") { implicit ticker =>
    Ref.unsafe[IO, Int](0).runState(ev =>
      StatefulTests(ev).stateful[Int]
    )
  }

  checkAll("Ref.runTell") { implicit ticker =>
    Ref.unsafe[IO, Int](0).runTell(ev =>
      TellTests(ev).tell[Int]
    )
  }

  checkAll("Consumer.runTell") { implicit ticker =>
    case object DummyErr extends Throwable
    def fun(x: Int) =
      if (x == 1) IO.raiseError[Unit](DummyErr)
      else IO.unit
    Consumer(fun _).runTell(ev =>
      TellTests(ev).tell[Int]
    )

  }
}
