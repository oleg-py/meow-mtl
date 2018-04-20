package com.olegpy.meow.internal

import shapeless._

class MkLensToType[S, A](lens: Lens[S, A]) {
  def apply(): Lens[S, A] = lens
}

trait AutoLensLP0 {
//  implicit def root[S]: MkLensToType[S, S] = new MkLensToType(OpticDefns.id[S])

  implicit def hlistElem[L <: HList, A](
    implicit
    mkHListSelectLens: MkHListSelectLens[L, A]): MkLensToType[L, A] =
    new MkLensToType(mkHListSelectLens())
}

trait AutoLensLP1 extends AutoLensLP0 {
  implicit def deriveInstance[A, L, S](
    implicit
    gen: MkGenericLens.Aux[A, L],
    ll: Lazy[MkLensToType[L, S]]): MkLensToType[A, S] =
    new MkLensToType(ll.value() compose gen())
}

trait AutoLensLP2 extends AutoLensLP1 {
  implicit def deriveTail[H, T <: HList, A](
    implicit
    ll: Lazy[MkLensToType[T, A]]): MkLensToType[H :: T, A] =
    new MkLensToType(new Lens[H :: T, A] {
      private[this] val tlz = ll.value()

      def get(s: H :: T): A = tlz.get(s.tail)
      def set(s: H :: T)(a: A): H :: T = s.head :: tlz.set(s.tail)(a)
    })
}

object MkLensToType extends AutoLensLP2 {
  implicit def deriveHead[H, T <: HList, A](
    implicit
    ll: Lazy[MkLensToType[H, A]]): MkLensToType[H :: T, A] =
    new MkLensToType(new Lens[H :: T, A] {
      private[this] val hlz = ll.value()

      def get(s: H :: T): A = hlz.get(s.head)
      def set(s: H :: T)(a: A): H :: T = hlz.set(s.head)(a) :: s.tail
    })
}
