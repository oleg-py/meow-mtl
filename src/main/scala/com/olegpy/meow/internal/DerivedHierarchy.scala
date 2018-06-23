package com.olegpy.meow.internal

import cats.mtl._
import cats.mtl.hierarchy.{base => conv}
import cats.{ApplicativeError, MonadError}
import com.olegpy.meow.optics.{MkLensToType, MkPrismToType}
import shapeless.=:!=

private[meow] trait DerivedHierarchy extends DerivedHierarchy.Priority0

private[meow] object DerivedHierarchy {
  trait Priority0 extends Priority1 {
    implicit def deriveMonadState[F[_], S, A](implicit
      isAbstractF: IsAbstract[F],
      parent: MonadState[F, S],
      neq: S =:!= A,
      mkLensToType: MkLensToType[S, A]
    ): MonadState[F, A] =
      new StateOptics.Monad(parent, mkLensToType())

    implicit def deriveMonadError[F[_], S, A](implicit
      isAbstractF: IsAbstract[F],
      parent: MonadError[F, S],
      neq: S =:!= A,
      mkPrismToType: MkPrismToType[S, A]
    ): MonadError[F, A] =
      new RaiseOptics.Monad(parent, mkPrismToType())

    implicit def deriveFunctorTell[F[_], S, A](implicit
      isAbstractF: IsAbstract[F],
      parent: FunctorTell[F, S],
      neq: S =:!= A,
      mkPrismToType: MkPrismToType[S, A]
    ): FunctorTell[F, A] =
      new TellOptics.Functor(parent, mkPrismToType())
  }

  trait Priority1 extends Priority2 {
    implicit def askFromState[F[_], L](implicit ev: MonadState[F, L]): ApplicativeAsk[F, L] =
      conv.askFromState[F, L]

    implicit def tellFromState[F[_], L](implicit ev: MonadState[F, L]): FunctorTell[F, L] =
      conv.tellFromState[F, L]

    implicit def deriveApplicativeLocal[F[_], S, A](implicit
      isAbstractF: IsAbstract[F],
      parent: ApplicativeLocal[F, S],
      neq: S =:!= A,
      mkLensToType: MkLensToType[S, A]
    ): ApplicativeLocal[F, A] =
      new LocalOptics.Applicative(parent, mkLensToType())


    implicit def deriveApplicativeError[F[_], S, A](implicit
      isAbstractF: IsAbstract[F],
      parent: ApplicativeError[F, S],
      neq: S =:!= A,
      mkPrismToType: MkPrismToType[S, A]
    ): ApplicativeError[F, A] =
      new RaiseOptics.Applicative(parent, mkPrismToType())

  }

  trait Priority2 {
    implicit final def functorEmptyFromTraverseEmpty[F[_]](implicit F: TraverseEmpty[F]): FunctorEmpty[F] =
      conv.functorEmptyFromTraverseEmpty(F)

    implicit final def askFromLocal[F[_], E](implicit local: ApplicativeLocal[F, E]): ApplicativeAsk[F, E] =
      conv.askFromLocal(local)

    implicit final def tellFromListen[F[_], L](implicit listen: FunctorListen[F, L]): FunctorTell[F, L] =
      conv.tellFromListen(listen)

    implicit def deriveApplicativeAsk[F[_], S, A](implicit
      isAbstractF: IsAbstract[F],
      parent: ApplicativeAsk[F, S],
      neq: S =:!= A,
      mkLensToType: MkLensToType[S, A]
    ): ApplicativeAsk[F, A] =
      new AskOptics.Applicative(parent, mkLensToType())

    implicit def deriveFunctorRaise[F[_], S, A](implicit
      isAbstractF: IsAbstract[F],
      parent: FunctorRaise[F, S],
      neq: S =:!= A,
      mkPrismToType: MkPrismToType[S, A]
    ): FunctorRaise[F, A] =
      new RaiseOptics.Functor(parent, mkPrismToType())
  }
}
