package com.olegpy.meow.optics

import shapeless._


class MkIsoToType[S, A](iso: TIso[S, A]) {
  def apply(): TIso[S, A] = iso
}

object MkIsoToType {
  implicit def refl[S]: MkIsoToType[S, S] =
    new MkIsoToType(new TIso[S, S] {
      def from(a: S): S = a
      def to(b: S): S = b
    })

  implicit def singleElementProduct[S, A](implicit
    gen: Generic.Aux[S, A :: HNil]
  ): MkIsoToType[S, A] = new MkIsoToType(new TIso[S, A] {
    def from(a: S): A = gen.to(a).head
    def to(b: A): S = gen.from(b :: HNil)
  })
}