package com.olegpy.meow.internal

import cats.{Applicative, Functor, Monad}
import cats.effect.concurrent.Ref
import cats.kernel.Semigroup
import cats.mtl._
import cats.syntax.functor._
import cats.syntax.semigroup._

private[meow] object CatsEffectMtlInstances {
  class RefMonadState[F[_]: Monad, S](ref: Ref[F, S]) extends MonadState[F, S] {
    val monad: Monad[F] = implicitly
    def get: F[S] = ref.get
    def set(s: S): F[Unit] = ref.set(s)
    def inspect[A](f: S => A): F[A] = ref.get.map(f)
    def modify(f: S => S): F[Unit] = ref.update(f)
  }

  class RefFunctorTell[F[_]: Functor, L: Semigroup](ref: Ref[F, L])
    extends FunctorTell[F, L] with DefaultFunctorTell[F, L] {
    val functor: Functor[F] = implicitly
    def tell(l: L): F[Unit] = ref.update(_ |+| l)
  }

  class RefApplicativeAsk[F[_]: Applicative, S](ref: Ref[F, S])
    extends ApplicativeAsk[F, S] with DefaultApplicativeAsk[F, S] {
    val applicative: Applicative[F] = implicitly
    def ask: F[S] = ref.get
  }
}
