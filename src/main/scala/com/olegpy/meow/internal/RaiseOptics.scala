package com.olegpy.meow.internal

import cats.mtl.FunctorRaise
import cats.{ApplicativeError, MonadError}
import com.olegpy.meow.optics.TPrism


private[meow] object RaiseOptics {
  class Functor[F[_], S, E](
    parent: FunctorRaise[F, S],
    prism: TPrism[S, E]
  ) extends FunctorRaise[F, E] {
    val functor: cats.Functor[F] = parent.functor
    def raise[A](e: E): F[A] = parent.raise(prism(e))
  }

  class Applicative[F[_], S, E](
    parent: ApplicativeError[F, S],
    prism: TPrism[S, E]
  ) extends ApplicativeError[F, E] {
    def raiseError[A](e: E): F[A] = parent.raiseError(prism(e))
    def handleErrorWith[A](fa: F[A])(f: E => F[A]): F[A] =
      parent.recoverWith(fa) { case prism(e) => f(e) }

    def pure[A](x: A): F[A] = parent.pure(x)
    def ap[A, B](ff: F[A => B])(fa: F[A]): F[B] = parent.ap(ff)(fa)
  }

  class Monad[F[_], S, E](
    parent: MonadError[F, S],
    prism: TPrism[S, E]
  ) extends Applicative(parent, prism) with MonadError[F, E] {
    def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = parent.flatMap(fa)(f)
    def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] = parent.tailRecM(a)(f)
  }
}
