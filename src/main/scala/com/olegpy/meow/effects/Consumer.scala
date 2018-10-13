package com.olegpy.meow.effects

import cats.Functor
import cats.mtl.{DefaultFunctorTell, FunctorTell}


/**
 * A wrapper for effectful function `A => F[Unit]`
 */
final class Consumer[F[_], A] private (val consume: A => F[Unit]) extends AnyVal {
  /**
   * Execute an operation that can "log" values of type `A` using this [[Consumer]]
   *
   * N.B.: unlike most FunctorTell instances, this one does not require
   * any constraints on `A`
   *
   * As an example, a simple async logger that only blocks if a previous message is
   * still being processed, to ensure correct ordering:
   *
   * {{{
   *   def greeter(name: String)(implicit ev: FunctorTell[IO, String]): IO[Unit] =
   *     ev.tell(s"Long time no see, \$name") >> IO.sleep(1.second)
   *
   *   def forever[A](ioa: IO[A]): IO[Nothing] = ioa >> forever(ioa)
   *
   *   for {
   *      mVar <- MVar.empty[IO, String]
   *      logger = forever(mVar.take.flatMap(s => IO(println(s)))
   *      _ <- logger.start // Do logging in background
   *      _ <- Consumer(mVar.put).runTell { implicit tell =>
   *        forever(greeter("Oleg"))
   *      }
   *   } yield ()
   * }}}
   */
  def runTell[B](f: FunctorTell[F, A] => B)(implicit F: Functor[F]): B =
    f(new Consumer.TellInstance(this))

  /**
   * Directly return an instance for `FunctorTell` that is based on this `Consumer`
   *
   * Returned instance would call the `Consumer` function as its `tell` operation
   *
   * @see [[runTell]] for potentially more convenient usage
   */
  def tellInstance(implicit F: Functor[F]): FunctorTell[F, A] = runTell(identity)
}

object Consumer {
  /**
   * Creates a new [[Consumer]] out of provided effectful function
   */
  def apply[F[_], A](f: A => F[Unit]): Consumer[F, A] = new Consumer(f)


  private class TellInstance[F[_]: Functor, A](c: Consumer[F, A])
    extends FunctorTell[F, A]
      with DefaultFunctorTell[F, A] {
    val functor: Functor[F] = implicitly
    def tell(l: A): F[Unit] = c.consume(l)
  }
}
