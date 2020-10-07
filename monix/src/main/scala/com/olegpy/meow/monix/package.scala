package com.olegpy.meow

import _root_.monix.eval.{Task, TaskLocal}
import cats.Semigroup
import cats.mtl.{Local, Tell, Stateful}
import internal.MonixMtlInstances._

/**
 * Provides Tell, Local and Stateful for TaskLocal
 */
package object monix {
  implicit class TaskLocalEffects[A](val self: TaskLocal[A]) {
    def runState[B](f: Stateful[Task, A] => B): B =
      f(new TaskLocalStateful(self))

    def stateInstance: Stateful[Task, A] =
      runState(identity)

    def runLocal[B](f: Local[Task, A] => B): B =
      f(new TaskLocalLocal(self))

    def localInstance: Local[Task, A] =
      runLocal(identity)

    def runTell[B](f: Tell[Task, A] => B)(implicit A: Semigroup[A]): B =
      f(new TaskLocalTell(self))

    def tellInstance(implicit A: Semigroup[A]): Tell[Task, A] =
      runTell(identity)

  }
}
