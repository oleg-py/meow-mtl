package com.olegpy.meow.internal

import cats.mtl.ApplicativeAsk
import cats.syntax.all._
import shapeless.Lens

object AskOptics {
  class Applicative[F[_], E, A](
    parent: ApplicativeAsk[F, E],
    lens: Lens[E, A]
  ) extends ApplicativeAsk[F, A] {
    implicit val applicative: cats.Applicative[F] = parent.applicative
    def ask: F[A] = parent.ask map lens.get
    def reader[B](f: A => B): F[B] = parent.reader(f compose lens.get)
  }
}
