package com.olegpy.meow.internal

import cats.mtl.{Handle}
import com.olegpy.meow.optics.TPrism


private[meow] object HandleOptics {
  class Applicative[F[_], S, E](
    parent: Handle[F, S],
    prism: TPrism[S, E]
  ) extends Handle[F, E] {
    val applicative: cats.Applicative[F] = parent.applicative

    def handleWith[A](fa: F[A])(f: E => F[A]): F[A] =
      parent.handleWith(fa) {
        case prism(e) => f(e)
        case e => parent.raise(e)
      }

    def raise[E2 <: E, A](e: E2): F[A] = parent.raise(prism(e))
  }
}
