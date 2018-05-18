package com.olegpy.meow.effects

import cats.Applicative
import cats.effect.{Async, Concurrent, Sync}
import cats.effect.concurrent.{Ref, Semaphore}
import cats.mtl.{ApplicativeAsk, DefaultApplicativeAsk}
import cats.syntax.all._
import shapeless.tag
import shapeless.tag.@@


/**
 * A memoized effectful value.
 */
final class Once[F[_], A] private (val get: F[A]) {
  /**
   * Enables [[runAsk]] operation on this [[Once]] instance.
   *
   * Note that when using it on a lazily memoized value, `runAsk`
   * would violate the law of `ask` adding no effects:
   * {{{ ask *> fa <-> fa }}}
   *
   * They would still, however, satisfy a weaker law of adding an effect once:
   *
   * {{{ ask *> ask *> fa <-> ask *> fa }}}
   *
   * and can be desired in settings where a dependency construction is expensive,
   * but optional.
   *
   * Because of such, operation requires explicit action from user.
   *
   * @see [[Once$.eager]] builder for creating lawful instances
   */
  def unsafeAllowAsk: Once[F, A] @@ Once.CanAsk =
    tag[Once.CanAsk](this)

  /**
   * Execute an operation requiring some additional context `A`, provided by
   * this [[Once]] instance.
   */
  def runAsk[B](f: ApplicativeAsk[F, A] => B)(implicit
    F: Applicative[F],
    ev: this.type <:< Once[F, A] @@ Once.CanAsk
  ): B =
    f(new Once.AskInstance(this))
}

object Once {
  sealed trait CanAsk

  /**
   * A builder for [[Once]] values which are memoized on first access.
   *
   * These [[Once]] instances are not safe if accessed concurrently, but
   * require only `Sync` instance to be available.
   *
   * @see [[atomic]] for values to be used in concurrent setting
   * @see [[eager]] for values that are memoized eagerly
   */
  def apply[F[_], A](fa: F[A])(implicit F: Sync[F]): F[Once[F, A]] =
    for {
      ref <- Ref[F].of(fa) // fa here used as a dummy of needed type
      _   <- ref.set {
        fa.flatTap(a => ref.set(F.pure(a)))
      }
    } yield new Once(ref.get.flatten)

  /**
   * A builder for [[Once]] values which are memoized on first access and guarantee
   * at-most-once semantics even in concurrent environment
   *
   * Resulting values have an overhead of synchronization.
   *
   * @see [[apply]] for simpler instances that do not provide concurrency guarantee
   * @see [[eager]] for values that are memoized eagerly
   */
  def atomic[F[_], A](fa: F[A])(implicit F: Async[F]): F[Once[F, A]] =
    for {
      lock <- Semaphore.uncancelable(1)
      sync <- Once(fa)
    } yield new Once(lock.withPermit(sync.get))

  /**
   * A builder for [[Once]] values which are evaluated concurrently, potentially on
   * a different thread. Trying to access value will await for the evaluation to complete
   * first, if it hasn't yet.
   *
   * Produces [[Once]] values that are capable of doing a [[Once.runAsk]].
   *
   * @see [[apply]] for lazily-evaluated version
   * @see [[atomic]] for lazily evaluated and thread-safe version
   */
  def eager[F[_], A](fa: F[A])(implicit F: Concurrent[F]): F[Once[F, A] @@ CanAsk] =
    F.start(fa).map(fiber => new Once(fiber.join).unsafeAllowAsk)

  private class AskInstance[F[_]: Applicative, A](once: Once[F, A])
    extends ApplicativeAsk[F, A]
      with DefaultApplicativeAsk[F, A] {
    val applicative: Applicative[F] = implicitly
    def ask: F[A] = once.get
  }
}
