package com.olegpy.meow.optics

import shapeless._

class MkLensToType[S, A](lens: Lens[S, A]) {
  def apply(): Lens[S, A] = lens
}

private[meow] trait AutoLensLP0 {
  implicit def hlistElem[L <: HList, A](implicit
    mkHListSelectLens: MkHListSelectLens[L, A]
  ): MkLensToType[L, A] =
    new MkLensToType(mkHListSelectLens())
}

private[meow] trait AutoLensLP1 extends AutoLensLP0 {
  implicit def deriveInstance[A, L, S](implicit
    gen: MkGenericLens.Aux[A, L],
    ll: Lazy[MkLensToType[L, S]]
  ): MkLensToType[A, S] =
    new MkLensToType(ll.value() compose gen())
}

private[meow] trait AutoLensLP2 extends AutoLensLP1 {
  implicit def deriveTail[H, T <: HList, A](implicit
    ll: Lazy[MkLensToType[T, A]]
  ): MkLensToType[H :: T, A] =
    new MkLensToType(new Lens[H :: T, A] {
      private[this] val tlz = ll.value()

      def get(s: H :: T): A = tlz.get(s.tail)
      def set(s: H :: T)(a: A): H :: T = s.head :: tlz.set(s.tail)(a)
    })
}

private[meow] trait AutoLensLP3 extends AutoLensLP2 {
  implicit def deriveHead[H, T <: HList, A](
    implicit
    ll: Lazy[MkLensToType[H, A]]): MkLensToType[H :: T, A] =
    new MkLensToType(new Lens[H :: T, A] {
      private[this] val hlz = ll.value()

      def get(s: H :: T): A = hlz.get(s.head)
      def set(s: H :: T)(a: A): H :: T = hlz.set(s.head)(a) :: s.tail
    })
}

object MkLensToType extends AutoLensLP3 {
  implicit def fromIso[S, A](implicit mkIso: MkIsoToType[S, A]): MkLensToType[S, A] =
    new MkLensToType(mkIso().toLens)
}
