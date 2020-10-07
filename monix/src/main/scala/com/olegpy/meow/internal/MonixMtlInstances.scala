package com.olegpy.meow.internal

import cats.Semigroup
import cats.syntax.semigroup._
import cats.{Applicative, Functor, Monad}
import cats.mtl.{Local, Tell, Stateful}
import monix.eval.TaskLocal
import monix.eval.Task

private[meow] object MonixMtlInstances {
  class TaskLocalLocal[E](taskLocal: TaskLocal[E])
    extends Local[Task, E] {
    override def local[A](fa: Task[A])(f: E => E): Task[A] =
      taskLocal.bindL(taskLocal.read map f)(fa)

    override def scope[A](fa: Task[A])(e: E): Task[A] = taskLocal.bind(e)(fa)
    override val applicative: Applicative[Task] = Task.catsAsync
    override def ask[E2 >: E]: Task[E2] = taskLocal.read
    override def reader[A](f: E => A): Task[A] = ask.map(f)
  }

  class TaskLocalStateful[S](taskLocal: TaskLocal[S])
    extends Stateful[Task, S] {
    override val monad: Monad[Task] = Task.catsAsync
    override def get: Task[S] = taskLocal.read
    override def set(s: S): Task[Unit] = taskLocal.write(s)
  }

  class TaskLocalTell[E: Semigroup](taskLocal: TaskLocal[E])
    extends Tell[Task, E] {
    override val functor: Functor[Task] = Task.catsAsync
    override def tell(l: E): Task[Unit] =
      taskLocal.read.flatMap(e => taskLocal.write(e |+| l))
  }
}
