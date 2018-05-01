package com.olegpy.meow.optics

import shapeless._
import shapeless.ops.coproduct.{Inject, Selector}


class MkPrismToType[S, A](prism: TPrism[S, A]) {
  def apply(): TPrism[S, A] = prism
}

private[meow] trait AutoPrismLP0 {
  implicit def fromIso[S, A](implicit mkIso: MkIsoToType[S, A]): MkPrismToType[S, A] =
    new MkPrismToType(mkIso().toPrism)

  implicit def coproductElem[L <: Coproduct, A](
    implicit sel: Selector[L, A],
    inj: Inject[L, A]
  ): MkPrismToType[L, A] =
    new MkPrismToType(new TPrism[L, A] {
      def apply(a: A): L = inj(a)
      def unapply(s: L): Option[A] = sel(s)
    })
}

private[meow] trait AutoPrismLP1 extends AutoPrismLP0 {
  implicit def deriveInstance[A, L <: Coproduct, S](
    implicit
    gen: Generic.Aux[A, L],
    ll: Lazy[MkPrismToType[L, S]]): MkPrismToType[A, S] = {
    new MkPrismToType(new TPrism[A, S] {
      private[this] val prism = ll.value()
      def apply(a: S): A = gen.from(prism(a))
      def unapply(s: A): Option[S] = prism.unapply(gen.to(s))
    })
  }
}

private[meow] trait AutoPrismLP2 extends AutoPrismLP1 {
  implicit def deriveTail[H, T <: Coproduct, A](
    implicit ll: Lazy[MkPrismToType[T, A]]
  ): MkPrismToType[H :+: T, A] = {
    new MkPrismToType(new TPrism[H :+: T, A] {
      private[this] val prism = ll.value()
      def apply(a: A): H :+: T = Inr[H, T](prism(a))
      def unapply(s: H :+: T): Option[A] = s match {
        case Inr(tail) => prism.unapply(tail)
        case _ => None
      }
    })
  }
}

private[meow] trait AutoPrismLP3 extends AutoPrismLP2 {
  implicit def deriveHead[H, T <: Coproduct, A](
    implicit
    ll: Lazy[MkPrismToType[H, A]]
  ): MkPrismToType[H :+: T, A] = {
    new MkPrismToType(new TPrism[H :+: T, A] {
      private[this] val prism = ll.value()
      def apply(a: A): H :+: T = Inl(prism(a))
      def unapply(s: H :+: T): Option[A] = s match {
        case Inl(head) => prism.unapply(head)
        case _ => None
      }
    })
  }
}

object MkPrismToType extends AutoPrismLP3 {
  implicit def subtyping[A, B <: A](implicit
    notCoproduct: B <:!< Coproduct,
    typeable: Typeable[B]
  ): MkPrismToType[A, B] = {
    new MkPrismToType(new TPrism[A, B] {
      def apply(a: B): A = a
      def unapply(s: A): Option[B] = typeable.cast(s)
    })
  }
}
