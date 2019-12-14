# meow-mtl
![Maven central](https://img.shields.io/maven-central/v/com.olegpy/meow-mtl-core_2.13.svg?style=flat-square)

A catpanion library for [cats-mtl] and [cats-effect] providing:

- Easy composition of MTL-style functions
- MTL instances for cats-effect compatible datatypes (e.g. `IO`) and monix TaskLocal
- Conflict-free implicits for sub-instances (e.g. `MonadState` => `Monad`)

Available for Scala 2.11, 2.12 and 2.13, for Scala JVM and Scala.JS (0.6)

```scala
// Use %%% for scala.js or cross projects
// Classy lenses derivation (requires shapeless)
libraryDependencies += "com.olegpy" %% "meow-mtl-core" % "0.4.0"
// MTL instances for cats-effect Ref and effectful functions
libraryDependencies += "com.olegpy" %% "meow-mtl-effects" % "0.4.0"
// MTL instances for TaskLocal
libraryDependencies += "com.olegpy" %% "meow-mtl-monix" % "0.4.0"
```


Inspired by [Next-level MTL talk][mtl-talk] and discussions on cats gitter.

You can also see demonstration of techniques this library enables in [a post](https://typelevel.org/blog/2018/08/25/http4s-error-handling-mtl.html) or [a talk](https://www.youtube.com/watch?v=gYnbOUGpWK0) by [Gabriel Volpe](https://github.com/gvolpe)

### Quick Example
```scala

type Headers = Map[String, String]
case class User(name: String)
case class AuthedRequest(headers: Headers, user: User)

def greetUser[F[_]: Functor](implicit F: MonadState[F, User]): F[String] = {
  F.get.map(user => s"Hello, ${user.name}")
}

def addRequestIdHeader[F[_]: Sync](implicit F: MonadState[F, Headers]): F[Unit] =
  for {
    id <- Sync[F].delay(UUID.randomUUID().toString)
    _  <- F.modify(_ + ("X-Request-ID" -> id))
  } yield ()
```

Now, if you had `AuthedRequest` as a state, that *should* mean that you
have a state of `User` and `Headers` too. This library allows you to call these
functions directly:

```scala
import com.olegpy.meow.hierarchy._

def handleGreetRequest[F[_]: Sync](implicit F: MonadState[F, AuthedRequest]) =
  for {
    _ <- addRequestIdHeader[F]
    r <- greetUser[F]
  } yield r
```

To get that `MonadState` instance, it's possible to use `StateT`
transformer. But meow-mtl allows you to use `Ref` from cats-effect
instead, yielding better performance. So at the edge of your application
it is possible to do this:

```scala
import com.olegpy.meow.effects._

def handleRequest: IO[String] =
  for {
    ref <- Ref[IO].of(AuthedRequest(Map(), User("John")))
    res <- ref.runState { implicit monadState =>
      handleGreetRequest[IO]
    }
  } yield res
```

## Classy optics and MTL composition

Primary feature of meow-mtl is enabling boilerplate-free composition of
functions using cats-mtl typeclasses, in cases where instance clearly
either contains necessary fields (like State example above) or can be
converted to a necessary type. For example, it's possible to narrow
type of `MonadError` from `Throwable` to a custom exception type:

```scala
case class MyException(msg: String) extends Throwable

def handleOnlyMy[F[_], A](f: F[A], fallback: F[A])(implicit F: MonadError[F, MyException]) =
  f.handleErrorWith(_ => fallback)


val io: IO[Int] = ???
handleOnlyMy(io, 42)
```

This is witnessed by `Lens` and `Prism` optics that meow-mtl generates
when you try to make a call to such method.

As another neat example, generated typeclasses can be used as ad-hoc lenses

```scala
case class Part(int: Int)
case class Whole(part: Part)

def modify[F[_]: MonadState[?[_], Whole]] =
  MonadState[F, Part].set(Part(42)) // automatically "zooms" into Whole.part
```

### High-level API: automatic derivation

All automatic derivation requires is a single import:

```scala
import com.olegpy.meow.hierarchy._
```

This needs to be done in every file where your call requires deriving an
instance.

Supported typeclasses:

| Typeclass         | Required optic |
|-------------------|----------------|
| ApplicativeError  | Prism          |
| ApplicativeHandle | Prism          |
| MonadError        | Prism          |
| FunctorRaise      | Prism          |
| FunctorTell       | Prism          |
| ApplicativeAsk    | Lens           |
| ApplicativeLocal  | Lens           |
| MonadState        | Lens           |

#### IMPORTANT!

Don't use `cats.mtl.implicits._` or `cats.mtl.hierarchy.base._` imports.
Import `cats.mtl.instances.all._` and `cats.mtl.syntax.all._` if you
need it.

Failure to do this will result in ambiguous implicit instances.

In cats-mtl 0.4.0 hierarchy has been mostly replaced by subtyping. The
remaining hierarchy imports will possibly be [phased out](https://github.com/typelevel/cats-mtl/issues/31)

### Low-level API: optic providers

Alternatively, `com.olegpy.meow.optics` can be used directly:

```scala
case class User(name: String)
type HasUser[A] = MkLensToType[A, User]


def isFred[A](a: A)(implicit mkLens: HasUser[A]) =
  mkLens().get(a).name == "Fred"
```

In here, `mkLens` is an object with 0-args `apply` method, that creates
a shapeless Lens from A to User, e.g.:

```scala
case class RequestCtx(user: User, id: String)

assert { isFred(RequestCtx(User("Fred"), "0x42")) }
```

Prism works in similar way, but it's a custom class (not shapeless
Prism) with `apply` and `unapply` methods for construction and matching.

This is a very bare-bones implementation of optics, having only minimal
functionality needed to support automatic derivation without adding
extra dependencies. If you need a full-fledged optics library, consider
using [monocle] instead.

## Cats-effect instances

meow-mtl provides instances for cats-effect compatible data types like
cats-effect own `IO` or [monix] `Coeval` and `Task`. These instances
reside in `com.olegpy.meow.effects` package and provide a more flexible
and performant alternative to monad transformer stacks.


Because construction of such instances is typically effectful, they are
*locally scoped*. That means, instead of being available by importing,
they require a special method to be called with a lambda, which will
receive an instance, i.e.:

```scala
// `unsafe` is used for the sake of an example. I don't recommend doing that.
Ref.unsafe[IO, Int](0).runAsk { implicit askInstance =>
  ??? // ApplicativeAsk[IO, Int] is available in this scope
}
```

Alternatively, you can pull it out with specific methods if you intend
to use it explicitly or with better-monadic-for implicit patterns:

```scala
implicit val instance: MonadState[IO, Int] =
  Ref.unsafe[IO, Int](0).stateInstance

// MonadState available below
???
```

### Ref
`Ref` is a referentially transparent variable added in cats-effect
1.0.0-RC2. It supports `MonadState`, `ApplicativeAsk` and `FunctorTell`
effects (the latest requires a `Semigroup` instance for type of
contained data).

Instances are provided by extension methods `runState`, `runAsk` and
`runTell` respectively.

#### Example: counter

This is a simple example of using `MonadState` instance of `Ref`. Note
how updated state can be retrieved from `ref` after executing operation.

```scala
def getAndIncrement[F[_]: Apply](implicit MS: MonadState[F, Int]) =
  MS.get <* MS.modify(_ + 1)


for {
  ref <- Ref.of[IO](0)
  out <- ref.runState { implicit ms =>
    getAndIncrement[IO].replicateA(3).as("Done")
  }
  state <- ref.get
} yield (out, state) == ("Done", 3)
```

### Consumer

`Consumer` is a simple wrapper around `A => F[Unit]`. It supports a
single effect - `FunctorTell`, and can be used for things like logging,
persistence, notifications, etc.

`Consumer` instances are constructed with `apply` method on a companion.


#### Example: async logger

That logger only waits if a previous message is still being processed,
to ensure correct ordering:

```scala
 def greeter(name: String)(implicit ev: FunctorTell[IO, String]): IO[Unit] =
   ev.tell(s"Long time no see, \$name") >> IO.sleep(1.second)

 def forever[A](ioa: IO[A]): IO[Nothing] = ioa >> forever(ioa)

 for {
    mVar <- MVar.empty[IO, String]
    logger = forever(mVar.take.flatMap(s => IO(println(s)))
    _ <- logger.start // Do logging in background
    _ <- Consumer(mVar.put).runTell { implicit tell =>
      forever(greeter("Oleg"))
    }
 } yield ()
```

### TaskLocal
Similar to Ref, but with TaskLocal scoping it's possible to provide ApplicativeLocal

#### Example: request context

Here, you can build a middleware for a service that provides some additional data per call.
This can be used for e.g. generating request IDs in HTTP server.


```scala
def service[F[_]: Monad](greeting: String, print: String => F[Unit])(implicit ev: ApplicativeAsk[F, String]): F[Unit] =
  ev.ask.map(name => s"$greeting $name") >>= print

def middleware[F[_]: Monad, A](getName: F[String])(service: F[Unit])(implicit ev: ApplicativeLocal[F, String]) =
  getName.flatMap(n => ev.scope(n)(service))

// Can be looking up something in external system, or random ID
val getName = Task(if (Random.nextBoolean()) "Oleg" else "Olga")
def putStrLn(s: String) = Task(println(s))

val run =
  for {
    name <- TaskLocal("")
    // note that you can create service separately from middleware,
    // as long as they share the TaskLocal
    svc  = name.runLocal { implicit ev =>
      service[Task]("Hello,", putStrLn)
    }
    withRandomName = name.runLocal { implicit ev => middleware(getName) _ }
    // and run them in another place entirely that doesn't know about TaskLocal
    // Randomly prints "Hello, Oleg" or "Hello, Olga"
    _ <- withRandomName(svc)
    // prints "Hello, " since we don't set a context
    _ <- svc
  } yield ()


// Don't forget to enable Local support!
run.executeWithOptions(_.enableLocalContextPropagation)
```

## Sub-instances
meow-mtl also provides a set of implicits which let you use
`Monad`/`Applicative`/`Functor` instances if you have an MTL instance of
compatible type (e.g. `MonadState`/`ApplicativeAsk`/`FunctorTell`)

```scala
import cats.implicits._
import com.olegpy.meow.prelude._ // just this one import

// Can use pure and flatTap without having a Monad constraint or pull
// it out manually
def test[F[_]](implicit MS: MonadState[F, Int]): F[Int] =
  42.pure[F].flatTap(MS.set)
```

It uses `LowPriority` mechanism from `shapeless` to ensure that _having_
a constraint does not result in ambiguities:

```scala
import cats.effect.Sync
// Uses Sync as a Monad instance, instead of getting it from MonadState
def test2[F[_]: Sync](implicit MS: MonadState[F, Int]): F[Int] =
  42.pure[F].flatTap(MS.set)
```

## License
MIT


[cats-effect]: https://github.com/typelevel/cats-effect
[cats-mtl]: https://github.com/typelevel/cats-mtl
[monocle]: https://github.com/julien-truffaut/Monocle
[monix]: https://github.com/monix/monix
[mtl-talk]: https://www.youtube.com/watch?v=GZPup5Iuaqw
