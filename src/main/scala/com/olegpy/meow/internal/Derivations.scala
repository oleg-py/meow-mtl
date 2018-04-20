package com.olegpy.meow.internal

import cats.mtl._
import cats.{ApplicativeError, MonadError}
import com.olegpy.meow._
import shapeless.=:!=

trait Derivations extends Derivations.Priority0

object Derivations {
  trait Priority0 extends Priority1 {
    implicit def deriveMonadState[F[_], S, A: Not[S]#l](implicit
      parent: MonadState[F, S],
      mkLensToType: MkLensToType[S, A]
    ): MonadState[F, A] =
      new LensedState.Monad(parent, mkLensToType())


    implicit def deriveApplicativeLocal[F[_], E, A: Not[E]#l](implicit
      parent: ApplicativeLocal[F, E],
      mkLensToType: MkLensToType[E, A]
    ): ApplicativeLocal[F, A] =
      new LensedLocal.Applicative(parent, mkLensToType())


    implicit def deriveMonadError[F[_], S, E: Not[S]#l](implicit
      parent: MonadError[F, S],
      mkPrismToType: MkPrismToType[S, E]
    ): MonadError[F, E] =
      new PrismedRaise.Monad(parent, mkPrismToType())
  }
  trait Priority1 extends Priority2 {
    implicit def deriveApplicativeError[F[_], S, E: Not[S]#l](implicit
      parent: ApplicativeError[F, S],
      mkPrismToType: MkPrismToType[S, E]
    ): ApplicativeError[F, E] =
      new PrismedRaise.Applicative(parent, mkPrismToType())

    implicit def deriveApplicativeAsk[F[_], E, A](implicit
      parent: ApplicativeAsk[F, E],
      mkLensToType: MkLensToType[E, A]
    ): ApplicativeAsk[F, A] =
      new LensedAsk.Applicative(parent, mkLensToType())
  }
  trait Priority2 {
    implicit def deriveFunctorRaise[F[_], S, E](implicit
      parent: FunctorRaise[F, S],
      mkPrismToType: MkPrismToType[S, E],
      sne: S =:!= E
    ): FunctorRaise[F, E] =
      new PrismedRaise.Functor(parent, mkPrismToType())
  }
}
