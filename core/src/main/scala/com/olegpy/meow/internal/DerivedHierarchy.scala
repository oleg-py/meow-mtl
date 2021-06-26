package com.olegpy.meow.internal

import cats.mtl._
import cats.ApplicativeError
import cats.MonadError
import com.olegpy.meow.optics.MkLensToType
import com.olegpy.meow.optics.MkPrismToType
import shapeless.<:!<
import shapeless.=:!=
import shapeless.Coproduct
import shapeless.Refute
import shapeless.Typeable

import scala.language.experimental.macros

private[meow] trait DerivedHierarchy extends DerivedHierarchy.Priority0

private[meow] object DerivedHierarchy {

  trait Priority0 extends Priority1 {

    implicit def deriveStateful[F[_], S, A](
      implicit
      isAbstractF: IsAbstract[F],
      parent: Stateful[F, S],
      neq: S =:!= A,
      mkLensToType: MkLensToType[S, A]
    ): Stateful[F, A] =
      new StateOptics.Monad(parent, mkLensToType())

    implicit def deriveTell[F[_], S, A](
      implicit
      isAbstractF: IsAbstract[F],
      parent: Tell[F, S],
      neq: A <:!< S,
      mkPrismToType: MkPrismToType[S, A]
    ): Tell[F, A] =
      new TellOptics.Functor(parent, mkPrismToType())

    // A version for concrete F[_]s, but limited to Throwables
    implicit def deriveMonadErrorFromThrowable[F[_], E <: Throwable, A](
      implicit
      nab: Refute[IsAbstract[F]],
      parent: MonadError[F, Throwable],
      neq: Throwable =:!= E,
      nc: E <:!< Coproduct,
      typ: Typeable[E]
    ): MonadError[F, E] =
      deriveMonadError[F, Throwable, E]

  }

  trait Priority1 extends Priority2 {

    implicit def deriveMonadError[F[_], S, A](
      implicit
      isAbstractF: IsAbstract[F],
      parent: MonadError[F, S],
      neq: S =:!= A,
      mkPrismToType: MkPrismToType[S, A]
    ): MonadError[F, A] =
      new RaiseOptics.Monad(parent, mkPrismToType())

  }

  trait Priority2 extends Priority3 {
//    implicit def deriveAsk[F[_], S, A](implicit
//      isAbstractF: IsAbstract[F],
//      parent: Ask[F, S],
//      neq: S =:!= A,
//      mkLensToType: MkLensToType[S, A]
//    ): Ask[F, A] =
//      new AskOptics.Applicative(parent, mkLensToType())

    implicit def deriveAsk[F[_], A]: Ask[F, A] = macro Macros.deriveTypeclassFromParent[Ask[F, A]]

    implicit def deriveApplicativeError[F[_], S, A](
      implicit
      isAbstractF: IsAbstract[F],
      parent: ApplicativeError[F, S],
      neq: S =:!= A,
      mkPrismToType: MkPrismToType[S, A]
    ): ApplicativeError[F, A] =
      new RaiseOptics.Applicative(parent, mkPrismToType())

    implicit def deriveHandle[F[_], S, A](
      implicit
      isAbstractF: IsAbstract[F],
      parent: Handle[F, S],
      neq: S =:!= A,
      mkPrismToType: MkPrismToType[S, A]
    ): Handle[F, A] =
      new HandleOptics.Applicative(parent, mkPrismToType())

  }

  trait Priority3 extends Priority4 {

    implicit def deriveRaise[F[_], S, A](
      implicit
      isAbstractF: IsAbstract[F],
      parent: Raise[F, S],
      neq: A <:!< S,
      mkPrismToType: MkPrismToType[S, A]
    ): Raise[F, A] =
      new RaiseOptics.Functor(parent, mkPrismToType())

  }

  trait Priority4 {

    implicit def deriveLocal[F[_], S, A](
      implicit
      isAbstractF: IsAbstract[F],
      parent: Local[F, S],
      neq: S =:!= A,
      mkLensToType: MkLensToType[S, A]
    ): Local[F, A] =
      new LocalOptics.Applicative(parent, mkLensToType())

  }

}
