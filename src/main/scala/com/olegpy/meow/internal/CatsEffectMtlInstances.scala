package com.olegpy.meow.internal

import cats.{Applicative, Apply, Functor, Monad}
import cats.effect.concurrent.{Deferred, Ref}
import cats.kernel.{Monoid, Semigroup}
import cats.mtl._
import cats.syntax.functor._
import cats.syntax.semigroupal._
import cats.syntax.monoid._

object CatsEffectMtlInstances {
  class RefMonadState[F[_]: Monad, S](ref: Ref[F, S]) extends MonadState[F, S] {
    val monad: Monad[F] = implicitly
    def get: F[S] = ref.get
    def set(s: S): F[Unit] = ref.set(s)
    def inspect[A](f: S => A): F[A] = ref.get.map(f)
    def modify(f: S => S): F[Unit] = ref.update(f)
  }

  class RefFunctorListen[F[_]: Apply, L: Semigroup](ref: Ref[F, L])
    extends FunctorListen[F, L] {
    val tell: FunctorTell[F, L] = new FunctorTell[F, L] with DefaultFunctorTell[F, L] {
      val functor: Functor[F] = implicitly
      def tell(l: L): F[Unit] = ref.update(_ |+| l)
    }

    def listen[A](fa: F[A]): F[(A, L)] = fa product ref.get
    def listens[A, B](fa: F[A])(f: L => B): F[(A, B)] = fa product ref.get.map(f)
  }

  class RefApplicativeAsk[F[_]: Applicative, S](ref: Ref[F, S])
    extends ApplicativeAsk[F, S] with DefaultApplicativeAsk[F, S] {
    val applicative: Applicative[F] = implicitly
    def ask: F[S] = ref.get
  }

  class DeferredApplicativeAsk[F[_]: Applicative, A](deferred: Deferred[F, A])
    extends ApplicativeAsk[F, A] with DefaultApplicativeAsk[F, A] {
    val applicative: Applicative[F] = implicitly
    def ask: F[A] = deferred.get
  }
}
