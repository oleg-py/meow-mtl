package com.olegpy.meow.effects

import cats.effect.IO
import cats.effect.concurrent.Ref
import cats.instances.all._
import minitest._
import minitest.laws.Checkers
import cats.mtl.laws.discipline._
import cats.effect.laws.discipline.arbitrary._
import cats.effect.laws.util.TestInstances._
import cats.effect.laws.util.TestContext
import scala.concurrent.duration._
import org.typelevel.discipline.Laws


object EffectInstancesLawsSuite extends SimpleTestSuite with Checkers {
  private def checkAll(name: String)(ruleSet: TestContext => Laws#RuleSet) = {
    implicit val ctx = TestContext()

    for ((id, prop) <- ruleSet(ctx).all.properties)
      test(name + "." + id) {
        ctx.tick(1.day)
        check(prop)
      }
  }

  checkAll("Ref.runAsk") { implicit ctx =>
    Ref.unsafe[IO, Int](0).runAsk(ev =>
      ApplicativeAskTests(ev).applicativeAsk[Int])
  }

  checkAll("Ref.runState") { implicit ctx =>
    Ref.unsafe[IO, Int](0).runState(ev =>
      MonadStateTests(ev).monadState[Int]
    )
  }

  checkAll("Ref.runTell") { implicit ctx =>
    Ref.unsafe[IO, Int](0).runTell(ev =>
      FunctorTellTests(ev).functorTell[Int]
    )
  }

  checkAll("Consumer.runTell") { implicit ctx =>
    case object DummyErr extends Throwable
    def fun(x: Int) =
      if (x == 1) IO.raiseError[Unit](DummyErr)
      else IO.unit
    Consumer(fun _).runTell(ev =>
      FunctorTellTests(ev).functorTell[Int]
    )

  }
}

