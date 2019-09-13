package com.olegpy.meow.derivations

import cats.MonadError
import cats.implicits._

object NonAbstract {
  import com.olegpy.meow.hierarchy._
  type Fallible[A] = Either[Throwable, A]

  sealed trait AppError extends Throwable
  case class ADbError() extends AppError
  case class ANetworkError() extends AppError

  def testMonadError(): Unit = {
    def derives[S](implicit ev: MonadError[Fallible, S]): Unit = ()

    derives[AppError]
    derives[ADbError]
    derives[ANetworkError]
  }
}
