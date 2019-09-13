package com.olegpy.meow

import _root_.monix.eval.{Task, TaskLocal}
import cats.Semigroup
import cats.mtl.{ApplicativeAsk, FunctorTell, MonadState}
import internal.MonixMtlInstances._

/**
 * Provides FunctorTell, ApplicativeLocal and MonadState for TaskLocal
 */
package object monix {
  implicit class TaskLocalEffects[A](val self: TaskLocal[A]) {
    def runState[B](f: MonadState[Task, A] => B): B =
      f(new TaskLocalMonadState(self))

    def stateInstance: MonadState[Task, A] =
      runState(identity)

    def runAsk[B](f: ApplicativeAsk[Task, A] => B): B =
      f(new TaskLocalApplicativeAsk(self))

    def askInstance: ApplicativeAsk[Task, A] =
      runAsk(identity)

    def runTell[B](f: FunctorTell[Task, A] => B)(implicit A: Semigroup[A]): B =
      f(new TaskLocalFunctorTell(self))

    def tellInstance(implicit A: Semigroup[A]): FunctorTell[Task, A] =
      runTell(identity)

  }
}
