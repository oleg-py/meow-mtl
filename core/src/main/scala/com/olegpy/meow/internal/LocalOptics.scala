package com.olegpy.meow.internal

import cats.mtl.Local
import shapeless.Lens

private[meow] object LocalOptics {
  class Applicative[F[_], E, A](
    parent: Local[F, E],
    lens: Lens[E, A]
  ) extends AskOptics.Applicative(parent, lens) with Local[F, A] {
    def local[B](fb: F[B])(f: A => A): F[B] =
      parent.local(fb)(lens.modify(_)(f))
  }
}
