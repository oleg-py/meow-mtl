# meow-mtl
A tiny library enabling easy composition of MTL-style functions. Provides:

- A mechanism of deriving optics based on types, a la classy lenses and prisms in Haskell
- An integration layer for [cats-mtl](https://github.com/typelevel/cats-mtl)

There are no published versions as of now. Stay tuned!

### Optics

**meow-mtl** contains a small optics package suited to its needs. These are contained in package `com.olegpy.meow.optics`.

Only the bare-bones types are provided: `TLens` (which is an alias for `shapeless.Lens`), `TPrism` and `TIso`, together with implicits:

- MkIsoToType[S, A] - available if `A = S` or if `S` is a single-element product type (e.g. `AnyVal` subclass)
- MkPrismToType[S, A] - available if `A <: S` or if S is a sealed trait that contains element `A`, or if `MkIsoToType[S, A]` is available
- MkLensToType[S, A] - available only if there's a path from `S` to `A` accessible by inspecting `case class` fields only (ignoring collections and methods)

These implicits expose a single method `apply()`, which provides respective optic instance.

### Instances

**meow-mtl** provides a replacement for `cats.mtl.hierarchy._` import:

```scala
import com.olegpy.meow.hierarchy._
```

It contains whole hierarchy of MTL typeclasses, with instances derived based on optics having higher priority than subtyping.
As a bonus, derived instances for `ApplicativeError` and `MonadError` of [cats](https://github.com/typelevel/cats) are provided as well.

#### Caveats ####
Currently it's not possible to use meow's hierarchy together with cats-mtl instances.
The workaround is to split code using concrete effect types with code using typeclass hierarchy into different modules / files.
Search for a solution is still ongoing. Using e.g. shapeless `Strict` results in exponential increase of compile times, which is not worth it.

#### Example ####
```scala
import cats.data._
import cats._
import cats.mtl._
import cats.syntax.functor._
import cats.syntax.flatMap._
import ApplicativeAsk._

final case class DbConfig(dbName: String)
final case class NetworkConfig(server: String)
final case class AppConfig(db: DbConfig, nc: NetworkConfig)

sealed trait DbError
sealed trait NetworkError

sealed trait AppError
final case class ADbError(e: DbError) extends AppError
final case class ANetworkError(e: NetworkError) extends AppError

object Main {
  object program {
    import com.olegpy.meow.hierarchy._

    def readFromDb[
    F[_]: Functor : FunctorRaise[?[_], DbError] : ApplicativeAsk[?[_], DbConfig],
    ]: F[String] =
      ask.map(_.dbName)

    def sendToNetwork[
    F[_]: Functor : FunctorRaise[?[_], NetworkError] : ApplicativeAsk[?[_], NetworkConfig],
    ](s: String): F[Unit] =
      ask.map(_.server + s).map(println)

    // Note that FunctorRaise instances for NetworkError and DbError,
    // as well as ApplicativeAsk instances for NetworkConfig and DbConfig
    // are automatically available without having to do anything manually
    def readAndSend[
    F[_]: Monad : FunctorRaise[?[_], AppError] : ApplicativeAsk[?[_], AppConfig]
    ]: F[Unit] = for {
      s <- readFromDb[F]
      _ <- sendToNetwork[F](s)
    } yield ()
  }

  def main(args: Array[String]): Unit = {
    import cats.mtl.implicits._ // notice that the instanes are imported only here - see caveat described above
    type T[X] = EitherT[Reader[AppConfig, ?], AppError, X]
    val start = AppConfig(DbConfig("db"), NetworkConfig("nc"))
    program.readAndSend[T].value(start)

    println("hoi")
  }
}
```
