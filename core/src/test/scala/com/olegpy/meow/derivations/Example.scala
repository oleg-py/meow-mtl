package com.olegpy.meow.derivations

import cats._
import cats.data._
import cats.mtl.Ask._
import cats.mtl._
import cats.mtl.instances.all._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.olegpy.meow.hierarchy._

final case class DbConfig(dbName: String)
final case class NetworkConfig(server: String)
final case class AppConfig(db: DbConfig, nc: NetworkConfig)

sealed trait DbError
sealed trait NetworkError

sealed trait AppError
final case class ADbError(e: DbError) extends AppError
final case class ANetworkError(e: NetworkError) extends AppError

// This example serves as a compile-time test too.
object Main {
  def readFromDb[
  F[_]: Functor : Raise[*[_], DbError] : Ask[*[_], DbConfig]
  ]: F[String] =
    askF[F]().map(_.dbName)

  def sendToNetwork[
  F[_]: Functor : Raise[*[_], NetworkError] : Ask[*[_], NetworkConfig]
  ](s: String): F[Unit] =
    askF[F]().map(_.server + s).map(println)

  def readAndSend[
  F[_]: Monad : Raise[*[_], AppError] : Ask[*[_], AppConfig]
  ]: F[Unit] = for {
    s <- readFromDb[F]
    _ <- sendToNetwork[F](s)
  } yield ()

  def main(args: Array[String]): Unit = {
    type T[X] = EitherT[Reader[AppConfig, *], AppError, X]
    val start = AppConfig(DbConfig("db"), NetworkConfig("nc"))
    readAndSend[T].value(start)

    println("hoi")
  }
}
