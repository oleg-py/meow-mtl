package com.olegpy.meow.effects

import cats.Functor
import cats.mtl.{DefaultFunctorTell, FunctorTell}


/**
 * A wrapper for effectful function `A => F[Unit]`
 */
final class Consumer[F[_], A] private (val consume: A => F[Unit]) extends AnyVal {
  def runTell[B](f: FunctorTell[F, A] => B)(implicit F: Functor[F]): B =
    f(new Consumer.TellInstance(this))
}

object Consumer {
  /**
   * Creates a new [[Consumer]] out of provided effectful function
   */
  def apply[F[_], A](f: A => F[Unit]): Consumer[F, A] = new Consumer(f)


  // TODO - check if it works as implicit conversion
  private class TellInstance[F[_]: Functor, A](c: Consumer[F, A])
    extends FunctorTell[F, A]
      with DefaultFunctorTell[F, A] {
    val functor: Functor[F] = implicitly
    def tell(l: A): F[Unit] = c.consume(l)
  }
}
