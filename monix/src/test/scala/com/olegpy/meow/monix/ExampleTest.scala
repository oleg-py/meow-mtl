package com.olegpy.meow.monix

import scala.util.Random

import cats.Monad
import cats.mtl.{ApplicativeAsk, ApplicativeLocal}
import cats.implicits._
import _root_.monix.eval.{Task, TaskLocal}

// Compile-time test for doc example
object ExampleTest {
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
}
