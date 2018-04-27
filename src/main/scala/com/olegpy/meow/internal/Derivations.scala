package com.olegpy.meow.internal

import cats.mtl._
import cats.{ApplicativeError, MonadError}
import com.olegpy.meow._
import com.olegpy.meow.optics.{MkLensToType, MkPrismToType}
import shapeless.{=:!=, Lazy}

trait Derivations extends Derivations.Priority0

object Derivations {
  trait Priority0 extends Priority1 {
    implicit def deriveMonadState[F[_], S, A](implicit
      parent: MonadState[F, S],
      mkLensToType: MkLensToType[S, A],
      neq: S =:!= A,
    ): MonadState[F, A] =
      new StateOptics.Monad(parent, mkLensToType())

    implicit def deriveMonadError[F[_], S, A](implicit
      parent: MonadError[F, S],
      mkPrismToType: MkPrismToType[S, A],
      neq: S =:!= A,
    ): MonadError[F, A] =
      new RaiseOptics.Monad(parent, mkPrismToType())
  }

  trait Priority1 extends Priority2 {
    implicit def deriveApplicativeLocal[F[_], S, A](implicit
      parent: ApplicativeLocal[F, S],
      mkLensToType: MkLensToType[S, A],
      neq: S =:!= A,
    ): ApplicativeLocal[F, A] =
      new LocalOptics.Applicative(parent, mkLensToType())


    implicit def deriveApplicativeError[F[_], S, A](implicit
      parent: ApplicativeError[F, S],
      mkPrismToType: MkPrismToType[S, A],
      neq: S =:!= A,
    ): ApplicativeError[F, A] =
      new RaiseOptics.Applicative(parent, mkPrismToType())

  }

  trait Priority2 {
    implicit def deriveApplicativeAsk[F[_], S, A](implicit
      parent: ApplicativeAsk[F, S],
      mkLensToType: MkLensToType[S, A],
      neq: S =:!= A,
    ): ApplicativeAsk[F, A] =
      new AskOptics.Applicative(parent, mkLensToType())

    implicit def deriveFunctorRaise[F[_], S, A](implicit
      parent: FunctorRaise[F, S],
      mkPrismToType: MkPrismToType[S, A],
      neq: S =:!= A,
    ): FunctorRaise[F, A] =
      new RaiseOptics.Functor(parent, mkPrismToType())
  }
}
