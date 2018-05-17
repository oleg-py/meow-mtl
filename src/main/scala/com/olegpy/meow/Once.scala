package com.olegpy.meow

import cats.Applicative
import cats.effect.concurrent.Ref
import cats.effect.Sync
import cats.mtl.{ApplicativeAsk, DefaultApplicativeAsk}
import cats.syntax.all._


final class Once[F[_], A] private (val get: F[A]) extends AnyVal {
  def runAsk[B](f: ApplicativeAsk[F, A] => B)(implicit F: Applicative[F]): B =
    f(new Once.AskInstance(this))
}

object Once {
  def apply[F[_], A](fa: F[A])(implicit F: Sync[F]): F[Once[F, A]] =
    for {
      ref <- Ref[F].of(fa) // fa is used as a dummy of needed type
      _   <- ref.set {
        fa.flatTap(a => ref.set(F.pure(a)))
      }
    } yield new Once(ref.get.flatten)

  private class AskInstance[F[_]: Applicative, A](once: Once[F, A])
    extends ApplicativeAsk[F, A]
      with DefaultApplicativeAsk[F, A] {
    val applicative: Applicative[F] = implicitly
    def ask: F[A] = once.get
  }
}
