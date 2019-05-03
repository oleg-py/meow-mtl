package com.olegpy.meow.internal

import cats.mtl.ApplicativeHandle
import com.olegpy.meow.optics.TPrism


private[meow] object HandleOptics {
  class Applicative[F[_], S, E](
    parent: ApplicativeHandle[F, S],
    prism: TPrism[S, E]
  ) extends ApplicativeHandle[F, E] {
    val applicative: cats.Applicative[F] = parent.applicative
    def handle[A](fa: F[A])(f: E => A): F[A] =
      parent.handle(fa) { case prism(e) => f(e) }

    def handleWith[A](fa: F[A])(f: E => F[A]): F[A] =
      parent.handleWith(fa) { case prism(e) => f(e) }

    def attempt[A](fa: F[A]): F[Either[E, A]] =
      functor.map(parent.attempt(fa)) {
        case Right(a) => Right(a)
        case Left(prism(e)) => Left(e)
      }

    val functor: cats.Functor[F] = parent.functor
    def raise[A](e: E): F[A] = parent.raise(prism(e))
  }
}
