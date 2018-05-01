package com.olegpy.meow.internal

import cats.mtl.MonadState
import cats.syntax.all._
import shapeless.Lens

private[meow] object StateOptics {
  class Monad[F[_], S, A](parent: MonadState[F, S], lens: Lens[S, A])
    extends MonadState[F, A] {
    implicit val monad: cats.Monad[F] = parent.monad
    def get: F[A] = parent.get map lens.get
    def set(a: A): F[Unit] = parent modify (lens.set(_)(a))
    def inspect[B](f: A => B): F[B] = get map f
    def modify(f: A => A): F[Unit] = parent modify (lens.modify(_)(f))
  }
}