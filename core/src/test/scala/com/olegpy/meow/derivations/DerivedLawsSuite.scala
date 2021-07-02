package com.olegpy.meow.derivations

import cats.{ApplicativeError, Eq, MonadError}
import cats.data.State
import cats.instances.all._
import cats.mtl._
import minitest._
import minitest.laws.Checkers
import org.typelevel.discipline.Laws
import com.olegpy.meow.hierarchy._
import cats.mtl.laws.discipline._
import cats.laws.discipline._
import cats.laws.discipline.arbitrary._
import cats.laws.discipline.eq._
import cats.Applicative

object DerivedLawsSuite extends SimpleTestSuite with Checkers {
  private def checkAll(name: String)(ruleSet: Laws#RuleSet) = {
    for ((id, prop) <- ruleSet.all.properties)
      test(name + "." + id) {
        check(prop)
      }
  }

  type Data = (MiniInt, Boolean)

  //temporarily stolen from https://github.com/typelevel/cats-mtl/pull/262/files
  implicit def localForFunction[E]: Local[E => *, E] =
    new Local[E => *, E] {
      def local[A](fa: E => A)(f: E => E): E => A = fa compose f
      val applicative: Applicative[E => *] = Applicative[E => *]
      def ask[E2 >: E]: E => E2 = identity[E]
    }

  checkAll("Stateful") {
    type M[A] = State[Data, A]
    def derive[F[_]](implicit MS: Stateful[F, Data]): Stateful[F, MiniInt] =
      implicitly

    implicit def eqState[A: Eq]: Eq[M[A]] = Eq.by(_.run((MiniInt.zero, false)))
    StatefulTests(derive[M]).stateful[Int]
  }

  checkAll("Local") {
    type M[A] = Data => A
    def derive[F[_]](implicit MS: Local[F, Data]): Local[F, MiniInt] =
      implicitly

    LocalTests(derive[M]).local[MiniInt, String]
  }

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

    HandleTests(derive[M]).handle[Int]
  }
}
