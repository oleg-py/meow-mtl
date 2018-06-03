package com.olegpy.meow.derivations

import cats.{ApplicativeError, MonadError}
import cats.mtl._


// This is a compile-time suite. If it compiles, it's fine.
object Chaining {
  import com.olegpy.meow.hierarchy._

  case class Inner(test: Long)
  case class StateComponent(nested: (String, Inner))
  case class State(number: Int, other: StateComponent)

  def testState[F[_]](implicit ev: MonadState[F, State]): Unit = {
    def derives[S](implicit ev: MonadState[F, S]): Unit = ()

    derives[State]
    derives[Inner]
    derives[StateComponent]
    derives[String]
    derives[Int]
    derives[Long]
  }

  def testLocal[F[_]](implicit ev: ApplicativeLocal[F, State]): Unit = {
    def derives[S](implicit ev: ApplicativeLocal[F, S]): Unit = ()

    derives[State]
    derives[Inner]
    derives[StateComponent]
    derives[String]
    derives[Int]
    derives[Long]
  }

  def testAsk[F[_]](implicit ev: ApplicativeAsk[F, State]): Unit = {
    def derives[S](implicit ev: ApplicativeAsk[F, S]): Unit = ()

    derives[State]
    derives[Inner]
    derives[StateComponent]
    derives[String]
    derives[Int]
    derives[Long]
  }

  def testStateToAsk[F[_]](implicit ev: MonadState[F, State]): Unit = {
    implicitly[ApplicativeAsk[F, State]]
    implicitly[ApplicativeAsk[F, Int]]
    ()
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

  def testFunctorRaise[F[_]](implicit ev: FunctorRaise[F, AppError]): Unit = {
    def derives[S](implicit ev: FunctorRaise[F, S]): Unit = ()

    derives[ADbError]
    derives[DbError]
    derives[ANetworkError]
    derives[NetworkError]
    derives[String]
  }
}
