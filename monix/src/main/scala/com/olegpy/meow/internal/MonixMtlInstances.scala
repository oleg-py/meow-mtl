package com.olegpy.meow.internal

import cats.Semigroup
import cats.syntax.semigroup._
import cats.{Applicative, Functor, Monad}
import cats.mtl.{ApplicativeLocal, DefaultFunctorTell, DefaultMonadState}
import monix.eval.TaskLocal
import monix.eval.Task

private[meow] object MonixMtlInstances {
  class TaskLocalApplicativeLocal[E](taskLocal: TaskLocal[E])
    extends ApplicativeLocal[Task, E] {
    override def local[A](f: E => E)(fa: Task[A]): Task[A] =
      taskLocal.bindL(taskLocal.read map f)(fa)

    override def scope[A](e: E)(fa: Task[A]): Task[A] = taskLocal.bind(e)(fa)
    override val applicative: Applicative[Task] = Task.catsAsync
    override def ask: Task[E] = taskLocal.read
    override def reader[A](f: E => A): Task[A] = ask.map(f)
  }

  class TaskLocalMonadState[S](taskLocal: TaskLocal[S])
    extends DefaultMonadState[Task, S] {
    override val monad: Monad[Task] = Task.catsAsync
    override def get: Task[S] = taskLocal.read
    override def set(s: S): Task[Unit] = taskLocal.write(s)
  }

  class TaskLocalFunctorTell[E: Semigroup](taskLocal: TaskLocal[E])
    extends DefaultFunctorTell[Task, E] {
    override val functor: Functor[Task] = Task.catsAsync
    override def tell(l: E): Task[Unit] =
      taskLocal.read.flatMap(e => taskLocal.write(e |+| l))
  }
}
