package com.olegpy.meow.derivations

import cats.effect.Sync
import cats.mtl.{ApplicativeAsk, MonadState}

// Check that syntax is enabled by having just the constraint
object Prelude {
  import cats.implicits._
  import com.olegpy.meow.prelude._

  def simpleChaining[F[_]](implicit MS: MonadState[F, Int]): F[Int] =
    42.pure[F].flatTap(MS.set)

  def simpleChaining[F[_]: Sync](implicit MS: MonadState[F, Int]): F[Int] =
    42.pure[F].flatTap(MS.set)

  def multipleInstances[F[_]](implicit MS: MonadState[F, Int], AA: ApplicativeAsk[F, String]): F[Int] =
    42.pure[F]
}
