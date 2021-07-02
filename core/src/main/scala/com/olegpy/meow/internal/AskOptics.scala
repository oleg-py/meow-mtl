package com.olegpy.meow.internal

import cats.mtl.Ask
import cats.syntax.all._
import com.olegpy.meow.optics.MkLensToType
import shapeless.<:!<
import shapeless.=:!=
import shapeless.Lens

object AskOptics {

  class Applicative[F[_], E, A](
    parent: Ask[F, E],
    lens: Lens[E, A]
  ) extends Ask[F, A] {
    implicit val applicative: cats.Applicative[F] = parent.applicative
    def ask[A2 >: A]: F[A2] = parent.ask map lens.get
  }

  case class Invariant[F[_], E](value: Ask[F, E]) extends AnyVal

  object Invariant {

    implicit def convert[F[_], A](implicit value: Ask[F, A]): Invariant[F, A] = Invariant(value)

    implicit def deriveInvariantAsk[F[_], S, A](
      implicit
      isAbstractF: IsAbstract[F],
      parent: AskOptics.Invariant[F, S],
      ns: S <:!< A,
      mkLensToType: MkLensToType[S, A]
    ): AskOptics.Invariant[F, A] =
      Invariant(new AskOptics.Applicative(parent.value, mkLensToType()))

  }

}
