package com.olegpy.meow.optics

abstract class TPrism[S, A] { outer =>
  def apply(a: A): S
  def unapply(s: S): Option[A]

  final def compose[K](other: TPrism[K, S]): TPrism[K, A] = new TPrism[K, A] {
    def apply(a: A): K = other(outer(a))
    def unapply(s: K): Option[A] = other.unapply(s).flatMap(outer.unapply)
  }
}
