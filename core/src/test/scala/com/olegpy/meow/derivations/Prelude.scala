package com.olegpy.meow.derivations

import cats.FlatMap
import cats.mtl.{Ask, Stateful}

// Check that syntax is enabled by having just the constraint
object Prelude {
  import cats.implicits._
  import com.olegpy.meow.prelude._

  def simpleChaining[F[_]](implicit MS: Stateful[F, Int]): F[Int] =
    42.pure[F].flatTap(MS.set)

  def simpleChaining[F[_]: FlatMap](implicit MS: Stateful[F, Int]): F[Int] =
    42.pure[F].flatTap(MS.set)

  def multipleInstances[F[_]](implicit MS: Stateful[F, Int], AA: Ask[F, String]): F[Int] =
    42.pure[F]
}
