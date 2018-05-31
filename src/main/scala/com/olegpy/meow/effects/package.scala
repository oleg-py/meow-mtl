package com.olegpy.meow

import cats.effect.concurrent.{Deferred, Ref}
import cats.mtl._
import cats.{Applicative, Monad, Semigroup}
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
     *   // TODO: example
     * }}}
     */
    def runAsk[B](f: ApplicativeAsk[F, A] => B)(implicit F: Applicative[F]): B =
      f(new RefApplicativeAsk(self))

    def runListen[B](f: FunctorListen[F, A] => B)(implicit F: Applicative[F], A: Semigroup[A]): B =
      f(new RefFunctorListen(self))
  }

  implicit class DeferredEffects[F[_], A](val self: Deferred[F, A]) extends AnyVal {
    def runAsk[B](f: ApplicativeAsk[F, A] => B)(implicit F: Applicative[F]): B =
      f(new DeferredApplicativeAsk(self))
  }
}
