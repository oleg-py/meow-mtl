package com.olegpy.meow.internal

import cats.data.Kleisli
import cats.instances.option._

/**
  * shapeless.Prism is actually an Optional - it doesn't have `construct` of desired form
  */
case class Prism[S, A](getOption: S => Option[A], reverseGet: A => S) {
  def unapply(s: S): Option[A] = getOption(s)

  final def andThen[B](other: Prism[A, B]): Prism[S, B] =
    Prism(
      Kleisli(getOption).andThen(other.getOption).run,
      other.reverseGet andThen reverseGet
    )

  final def compose[K](other: Prism[K, S]): Prism[K, A] = other andThen this
}

object Prism {
  def id[S]: Prism[S, S] = Prism(Some(_), identity)
}