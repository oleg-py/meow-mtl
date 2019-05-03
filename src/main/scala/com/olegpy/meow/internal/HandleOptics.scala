package com.olegpy.meow.internal

import cats.mtl.{ApplicativeHandle, DefaultApplicativeHandle}
import com.olegpy.meow.optics.TPrism


private[meow] object HandleOptics {
  class Applicative[F[_], S, E](
    parent: ApplicativeHandle[F, S],
    prism: TPrism[S, E]
  ) extends DefaultApplicativeHandle[F, E] {
    val applicative: cats.Applicative[F] = parent.applicative

    def handleWith[A](fa: F[A])(f: E => F[A]): F[A] =
      parent.handleWith(fa) {
        case prism(e) => f(e)
        case e => parent.raise(e)
      }

    val functor: cats.Functor[F] = parent.functor
    def raise[A](e: E): F[A] = parent.raise(prism(e))
  }
}
