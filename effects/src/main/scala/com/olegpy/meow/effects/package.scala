package com.olegpy.meow

import cats.effect.kernel.Ref
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
     *    def getAndIncrement[F[_]: Apply](implicit MS: Stateful[F, Int]) =
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
    def runState[B](f: Stateful[F, A] => B)(implicit F: Monad[F]): B =
      f(new RefStateful(self))

    /**
     * Directly return an instance for `Stateful` that is based on this `Ref`
     *
     * Returned instance would use `get`/`set` methods of this `Ref` to manipulate state
     *
     * @see [[runState]] for potentially more convenient usage
     */
    def stateInstance(implicit F: Monad[F]): Stateful[F, A] =
      runState(identity)

    /**
     * Execute an operation requiring some additional context `A` provided within this Ref.
     *
     * The value inside Ref cannot be modified by this operation (see [[runTell]] or
     * [[runState]] for such cases) but it can be modified concurrently by a forked task,
     * in which case reads will see the updated value.
     *
     * {{{
     *   case class RequestId(text: String)
     *
     *   def greet[F[_]: Sync: Ask[?[_], RequestId]](name: String): F[String] =
     *     for {
     *       rId <- Ask.askF[F]()
     *       _   <- Sync[F].delay(println(s"Handling request \$rId"))
     *     } yield s"Hello, \$name"
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
    def runAsk[B](f: Ask[F, A] => B)(implicit F: Applicative[F]): B =
      f(new RefAsk(self))

    /**
     * Directly return an instance for `Ask` that is based on this `Ref`
     *
     * Returned instance would use `get` method of this `Ref` to provide a value
     *
     * @see [[runAsk]] for potentially more convenient usage
     */
    def askInstance(implicit F: Applicative[F]): Ask[F, A] =
      runAsk(identity)

    /**
     * Execute an operation requiring ability to "log" values of type `A`, and,
     * potentially, read current value.
     *
     * The operation requires `A` to have a `Semigroup` instance. Unlike standard
     * `Writer` monad, initial (zero) is not required.
     *
     * {{{
     *   def generateUser[F[_]: Sync: Tell[?[_], String]](login: String) =
     *     for {
     *       _   <- tellF[F](s"Starting key generation for \$login")
     *       pwd <- IO(Random.alphanumeric.take(16).mkString)
     *       _   <- tellF[F](s"Generated key: \$key")
     *     } yield (login, pwd)
     *
     *
     *   for {
     *     ref  <- Ref[IO].of(NonEmptyList.of("Operation started"))
     *     user <- ref.runTell { implicit ft =>
     *       generateUser("Alice")
     *     }
     *     log <- ref.get
     *     _   <- IO(println(log))
     *   } yield user
     * }}}
     *
     * @see [[Consumer]] if you're interested in simply performing an operation
     *      on each `tell`
     *
     */
    def runTell[B](f: Tell[F, A] => B)(implicit F: Functor[F], A: Semigroup[A]): B =
      f(new RefTell(self))

    /**
     * Directly return an instance for `Tell` that is based on this `Ref`
     *
     * Returned instance would use `modify` method of this `Ref` and a `Semigroup`
     * to accumulate results
     *
     * @see [[runTell]] for potentially more convenient usage
     */
    def tellInstance(implicit F: Functor[F], A: Semigroup[A]): Tell[F, A] =
      runTell(identity)
  }
}
