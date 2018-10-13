package com.olegpy.meow.internal

import cats.mtl.ApplicativeLocal
import shapeless.Lens


private[meow] object LocalOptics {
  class Applicative[F[_], E, A](
    parent: ApplicativeLocal[F, E],
    lens: Lens[E, A]
  ) extends AskOptics.Applicative(parent, lens) with ApplicativeLocal[F, A] {
    def local[B](f: A => A)(fb: F[B]): F[B] =
      parent.local(lens.modify(_)(f))(fb)

    def scope[B](e: A)(fa: F[B]): F[B] = local(_ => e)(fa)
  }
}
