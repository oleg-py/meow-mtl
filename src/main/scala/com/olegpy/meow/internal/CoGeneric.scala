package com.olegpy.meow.internal

import shapeless._
import shapeless.ops.hlist.IsHCons

// An alternative to shapeless Generic that derives Coproduct only
// Instead of single-element product, we get a single-element coproduct
trait CoGeneric[A] {
  type Repr <: Coproduct
  def to(a: A): Repr
  def from(r: Repr): A
}

object CoGeneric {
  type Aux[A, R <: Coproduct] = CoGeneric[A] { type Repr = R }

  implicit def fromShapeless[A, R <: Coproduct](
    implicit gen: Generic.Aux[A, R]
  ): CoGeneric.Aux[A, R] = new CoGeneric[A] {
    type Repr = R
    def to(a: A): R = gen.to(a)
    def from(r: R): A = gen.from(r)
  }


  // Oddly, we cannot require Generic.Aux[A, E :: HNil], the implicit
  // search will not be able to instantiate the Generic for expected type
  implicit def fromSingle[A, R <: HList, E](implicit
    gen: Generic.Aux[A, R],
    ihc: IsHCons.Aux[R, E, HNil]
  ): CoGeneric.Aux[A, E :+: CNil] = new CoGeneric[A] {
    type Repr = E :+: CNil
    def to(a: A): Repr = Inl(gen.to(a).head)
    def from(r: Repr): A = r match {
      case Inl(head) => gen.from(ihc.cons(head, HNil))
      case Inr(tail) => tail.impossible
    }
  }
}
