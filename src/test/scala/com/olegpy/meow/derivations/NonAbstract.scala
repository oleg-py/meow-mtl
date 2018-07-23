package com.olegpy.meow.derivations

import cats.MonadError
import cats.effect.IO


object NonAbstract {
  import com.olegpy.meow.hierarchy._

  sealed trait AppError extends Throwable
  case class ADbError() extends AppError
  case class ANetworkError() extends AppError

  def testMonadError(): Unit = {
    def derives[S](implicit ev: MonadError[IO, S]): Unit = ()

    derives[AppError]
    derives[ADbError]
    derives[ANetworkError]
  }
}
