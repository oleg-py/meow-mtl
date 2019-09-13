package com.olegpy.meow.monix


import cats.implicits._
import cats.effect.laws.discipline.arbitrary.genIO
import cats.mtl.laws.discipline._
import minitest.SimpleTestSuite
import minitest.laws.Checkers
import org.typelevel.discipline.Laws
import scala.concurrent.duration._

import cats.Eq
import _root_.monix.eval.{Task, TaskLike, TaskLocal}
import _root_.monix.execution.schedulers.TestScheduler
import org.scalacheck.{Arbitrary, Cogen, Gen}

object MonixInstancesLawsSuite extends SimpleTestSuite with Checkers {
  private def checkAll(name: String)(ruleSet: TestScheduler => Laws#RuleSet) = {
    implicit val ctx = TestScheduler()

    for ((id, prop) <- ruleSet(ctx).all.properties)
      test(name + "." + id) {
        ctx.tick(1.day)
        check(prop)
      }
  }

  implicit val options: Task.Options = Task.Options(
    autoCancelableRunLoops = true,
    localContextPropagation = true
  )

  implicit val eqThrowable: Eq[Throwable] = Eq.allEqual

  implicit def eqTask[A](implicit
    eqA: Eq[A],
    ts: TestScheduler,
    options: Task.Options
  ): Eq[Task[A]] = Eq.instance { (lhs, rhs) =>
    val lf = lhs.runToFutureOpt
    val rf = rhs.runToFutureOpt
    ts.tick(1.day)
    lf.value.map(_.toEither) === rf.value.map(_.toEither)
  }

  implicit def arbitraryTask[A: Arbitrary: Cogen] =
    Arbitrary(Gen.delay(genIO[A].map(TaskLike.fromIO(_))))

  private def unsafeTaskLocal()(implicit ctx: TestScheduler) = {
    val f = TaskLocal(0).runToFutureOpt
    ctx.tick()
    f.value.get.get
  }

  checkAll("TaskLocal.runLocal") { implicit ctx =>
    unsafeTaskLocal().runLocal(ev =>
      ApplicativeLocalTests(ev).applicativeLocal[Int, String])
  }

  checkAll("TaskLocal.runState") { implicit ctx =>
    unsafeTaskLocal().runState(ev =>
      MonadStateTests(ev).monadState[Int]
    )
  }

  checkAll("TaskLocal.runTell") { implicit ctx =>
    unsafeTaskLocal().runTell(ev =>
      FunctorTellTests(ev).functorTell[Int]
    )
  }
}
