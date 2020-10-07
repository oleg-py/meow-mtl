package com.olegpy.meow.internal

import cats.mtl.Ask
import cats.syntax.all._
import shapeless.Lens

object AskOptics {
  class Applicative[F[_], E, A](
    parent: Ask[F, E],
    lens: Lens[E, A]
  ) extends Ask[F, A] {
    implicit val applicative: cats.Applicative[F] = parent.applicative
    def ask[A2 >: A]: F[A2] = parent.ask map lens.get
  }
}
