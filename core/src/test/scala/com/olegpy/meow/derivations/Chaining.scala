package com.olegpy.meow.derivations

import cats.ApplicativeError
import cats.MonadError
import cats.mtl._

// This is a compile-time suite. If it compiles, it's fine.
object Chaining {
  import com.olegpy.meow.hierarchy._

  case class Inner(test: Long)
  case class StateComponent(nested: (String, Inner))
  case class State(number: Int, other: StateComponent)

  implicit def ask: Ask[List, State] = ???

  def testState[F[_]](implicit ev: Stateful[F, State]): Unit = {
    def derives[S](implicit ev: Stateful[F, S]): Unit = ()

    derives[State]
    derives[Inner]
    derives[StateComponent]
    derives[String]
    derives[Int]
    derives[Long]
  }

  def testLocal[F[_]](implicit ev: Local[F, State]): Unit = {
    def derives[S](implicit ev: Local[F, S]): Unit = ()

    derives[State]
    derives[Inner]
    derives[StateComponent]
    derives[String]
    derives[Int]
    derives[Long]
  }

  def testAsk[F[_]](implicit ev: Ask[F, State]): Unit = {
    def derives[S](implicit ev: Ask[F, S]): Unit = ()

    derives[Any]
    derives[State]
    derives[Inner]
    derives[StateComponent]
    derives[String]
    derives[Int]
    derives[Long]
  }

  case class DbError(text: String)
  sealed trait NetworkError

  sealed trait AppError
  case class ADbError(e: DbError) extends AppError
  case class ANetworkError(e: NetworkError) extends AppError

  def testMonadError[F[_]](implicit ev: MonadError[F, AppError]): Unit = {
    def derives[S](implicit ev: MonadError[F, S]): Unit = ()

    derives[ADbError]
    derives[DbError]
    derives[ANetworkError]
    derives[NetworkError]
    derives[String]
  }

  def testApplicativeError[F[_]](implicit ev: ApplicativeError[F, AppError]): Unit = {
    def derives[S](implicit ev: ApplicativeError[F, S]): Unit = ()

    derives[ADbError]
    derives[DbError]
    derives[ANetworkError]
    derives[NetworkError]
    derives[String]
  }

  def testRaise[F[_]](implicit ev: Raise[F, AppError]): Unit = {
    def derives[S](implicit ev: Raise[F, S]): Unit = ()

    derives[ADbError]
    derives[DbError]
    derives[ANetworkError]
    derives[NetworkError]
    derives[String]
  }

  def testTell[F[_]](implicit ev: Tell[F, AppError]): Unit = {
    def derives[S](implicit ev: Tell[F, S]): Unit = ()

    derives[ADbError]
    derives[DbError]
    derives[ANetworkError]
    derives[NetworkError]
    derives[String]
  }

}
