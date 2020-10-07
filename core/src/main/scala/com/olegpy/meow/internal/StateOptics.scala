package com.olegpy.meow.internal

import cats.mtl.Stateful
import cats.syntax.all._
import shapeless.Lens

private[meow] object StateOptics {
  class Monad[F[_], S, A](parent: Stateful[F, S], lens: Lens[S, A])
    extends Stateful[F, A] {
    implicit val monad: cats.Monad[F] = parent.monad

    def get: F[A] = parent.get map lens.get
    def set(a: A): F[Unit] = parent modify (lens.set(_)(a))

    override def inspect[B](f: A => B): F[B] = get map f
    override def modify(f: A => A): F[Unit] = parent modify (lens.modify(_)(f))
  }
}
