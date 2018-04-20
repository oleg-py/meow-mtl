import cats.data._, cats._, cats.mtl._, cats.implicits._, ApplicativeAsk._, cats.mtl.implicits._
import com.olegpy.meow.instances._

final case class DbConfig(dbName: String)
final case class NetworkConfig(server: String)
final case class AppConfig(db: DbConfig, nc: NetworkConfig)

sealed trait DbError
sealed trait NetworkError

sealed trait AppError
final case class ADbError(e: DbError) extends AppError
final case class ANetworkError(e: NetworkError) extends AppError

object Main {

  def readFromDb[
  F[_]: Functor : FunctorRaise[?[_], DbError] : ApplicativeAsk[?[_], DbConfig],
  ]: F[String] =
    ask.map(_.dbName)

  def sendToNetwork[
  F[_]: Functor : FunctorRaise[?[_], NetworkError] : ApplicativeAsk[?[_], NetworkConfig],
  ](s: String): F[Unit] =
    ask.map(_.server + s).map(println)

  def readAndSend[
  F[_]: Monad : FunctorRaise[?[_], AppError] : ApplicativeAsk[?[_], AppConfig]
  ]: F[Unit] = for {
    s <- readFromDb[F]
    _ <- sendToNetwork[F](s)
  } yield ()

  def main(args: Array[String]): Unit = {
    type T[X] = EitherT[Reader[AppConfig, ?], AppError, X]
    val start = AppConfig(DbConfig("db"), NetworkConfig("nc"))
    readAndSend[T].value(start)

    println("hoi")
  }
}
