package com.olegpy.meow.derivations

import cats.{ApplicativeError, Eq, MonadError}
import cats.data.State
import cats.instances.all._
import cats.mtl._
import minitest._
import minitest.laws.Checkers
import org.typelevel.discipline.Laws
import com.olegpy.meow.hierarchy._
import cats.mtl.instances.all._
import cats.mtl.laws.discipline._
import cats.laws.discipline._
import cats.laws.discipline.arbitrary._
import cats.laws.discipline.eq._
import cats.laws.discipline.DeprecatedEqInstances._


object DerivedLawsSuite extends SimpleTestSuite with Checkers {
  private def checkAll(name: String)(ruleSet: Laws#RuleSet) = {
    for ((id, prop) <- ruleSet.all.properties)
      test(name + "." + id) {
        check(prop)
      }
  }

  type Data = (Long, Int)

  checkAll("Stateful") {
    type M[A] = State[Data, A]
    def derive[F[_]](implicit MS: Stateful[F, Data]): Stateful[F, Int] =
      implicitly

    implicit def eqState[A: Eq]: Eq[M[A]] = Eq.by(_.run((0L, 0)))
    StatefulTests(derive[M]).monadState[Int]
  }

  checkAll("Local") {
    type M[A] = Data => A
    def derive[F[_]](implicit MS: Local[F, Data]): Local[F, Int] =
      implicitly

    LocalTests(derive[M]).applicativeLocal[Int, String]
  }

//  checkAll("Ask") {
//    type M[A] = Data => A
//    def derive[F[_]](implicit MS: Ask[F, Data]): Ask[F, Int] =
//      implicitly
//
//    AskTests(derive[M]).applicativeAsk[Int]
//  }

  type DataC = Either[String, Either[Int, Long]]

  checkAll("MonadError") {
    type M[A] = Either[DataC, A]

    def derive[F[_]](implicit MS: MonadError[F, DataC]): MonadError[F, Long] =
      implicitly

    MonadErrorTests(derive[M]).monadError[Int, Long, String]
  }

  checkAll("ApplicativeError") {
    type M[A] = Either[DataC, A]

    def derive[F[_]](implicit MS: ApplicativeError[F, DataC]): ApplicativeError[F, Long] =
      implicitly

    ApplicativeErrorTests(derive[M]).applicativeError[Int, Long, String]
  }

  checkAll("Handle") {
    type M[A] = Either[DataC, A]

    def derive[F[_]](implicit MS: Handle[F, DataC]): Handle[F, Long] =
      implicitly

    HandleTests(derive[M]).applicativeHandle[Int]
  }
}
