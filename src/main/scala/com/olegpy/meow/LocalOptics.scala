package com.olegpy.meow

import cats.mtl.{ApplicativeAsk, ApplicativeLocal, DefaultApplicativeLocal}
import shapeless.Lens


object LocalOptics {
  class Applicative[F[_], E, A](
    parent: ApplicativeLocal[F, E],
    lens: Lens[E, A]
  ) extends ApplicativeLocal[F, A] with DefaultApplicativeLocal[F, A] {
    val ask: ApplicativeAsk[F, A] = new AskOptics.Applicative(parent.ask, lens)

    def local[B](f: A => A)(fb: F[B]): F[B] =
      parent.local(lens.modify(_)(f))(fb)
  }
}
