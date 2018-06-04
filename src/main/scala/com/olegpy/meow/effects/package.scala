package com.olegpy.meow

import cats.effect.concurrent.Ref
import cats.mtl._
import cats.{Applicative, Functor, Monad, Semigroup}
import com.olegpy.meow.internal.CatsEffectMtlInstances._

package object effects {
  implicit class RefEffects[F[_], A](val self: Ref[F, A]) extends AnyVal {
    /**
     * Execute a stateful operation using this `Ref` to store / update state.
     * The Ref will be modified to contain the resulting value. Returning value
     * would be a result of function passed to [[runState]].
     *
     * {{{
     *    def getAndIncrement[F[_]: Apply](implicit MS: MonadState[F, Int]) =
     *      MS.get <* MS.modify(_ + 1)
     *
     *
     *    for {
     *      ref <- Ref.of[IO](0)
     *      out <- ref.runState { implicit ms =>
     *        getAndIncrement[IO].replicateA(3).as("Done")
     *      }
     *      state <- ref.get
     *    } yield (out, state) == ("Done", 3)
     * }}}
     */
    def runState[B](f: MonadState[F, A] => B)(implicit F: Monad[F]): B =
      f(new RefMonadState(self))

    /**
     * Execute an operation requiring some additional context `A` provided within this Ref.
     *
     * The value inside Ref cannot be modified by this operation (see [[runListen]] or
     * [[runState]] for such cases) but it can be modified concurrently by a forked task,
     * in which case reads will see the updated value.
     *
     * {{{
     *   case class RequestId(text: String)
     *
     *   def greet[F[_]: Sync: ApplicativeAsk[?[_], RequestId]](name: String): F[String] =
     *     for {
     *       rId <- ApplicativeAsk.askF[F]()
     *       _   <- Sync[F].delay(println(s"Handling request $rId"))
     *     } yield s"Hello, $name"
     *
     *
     *   for {
     *     id  <- IO(UUID.randomUUID().toString).map(RequestId)
     *     ref <- Ref[IO].of(id)
     *     res <- ref.runAsk { implicit aa =>
     *       greet("Oleg")
     *     }
     *   } yield res
     * }}}
     */
    def runAsk[B](f: ApplicativeAsk[F, A] => B)(implicit F: Applicative[F]): B =
      f(new RefApplicativeAsk(self))

    /**
     * Execute an operation requiring ability to "log" values of type `A`, and,
     * potentially, read current value.
     *
     * The operation requires `A` to have a `Semigroup` instance. Unlike standard
     * `Writer` monad, initial (zero) is not required.
     *
     * {{{
     *   // TODO: example
     * }}}
     */
    def runTell[B](f: FunctorTell[F, A] => B)(implicit F: Functor[F], A: Semigroup[A]): B =
      f(new RefFunctorTell(self))
  }
}
