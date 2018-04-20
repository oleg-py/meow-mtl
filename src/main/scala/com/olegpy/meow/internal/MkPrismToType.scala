package com.olegpy.meow.internal

import shapeless._
import shapeless.ops.coproduct.{Inject, Selector}


class MkPrismToType[S, A](prism: Prism[S, A]) {
  def apply(): Prism[S, A] = prism
}

trait AutoPrismLP0 {
  implicit def loneElement[S, A](implicit
    gen: Generic.Aux[S, A :: HNil]
  ): MkPrismToType[S, A] =
    new MkPrismToType(Prism(s => Some(gen.to(s).head), a => gen.from(a :: HNil)))

  implicit def coproductElem[L <: Coproduct, A](
    implicit sel: Selector[L, A],
    inj: Inject[L, A]
  ): MkPrismToType[L, A] =
    new MkPrismToType(Prism(sel(_), inj(_)))
}

trait AutoPrismLP1 extends AutoPrismLP0 {
  implicit def deriveInstance[A, L <: Coproduct, S](
    implicit
    gen: Generic.Aux[A, L],
    ll: Lazy[MkPrismToType[L, S]]): MkPrismToType[A, S] = {
    def genericGetOption(a: A): Option[L] = Some(gen.to(a))
    def genericReverseGet(l: L): A = gen.from(l)
    val p = Prism(genericGetOption, genericReverseGet) andThen ll.value()
    new MkPrismToType(p)
  }
}

trait AutoPrismLP2 extends AutoPrismLP1 {
  implicit def deriveTail[H, T <: Coproduct, A](
    implicit ll: Lazy[MkPrismToType[T, A]]
  ): MkPrismToType[H :+: T, A] = {
    val tailPrism = ll.value()
    def reverseGet(a: A): H :+: T = Inr[H, T](tailPrism.reverseGet(a))
    def getOption(h: H :+: T) = h match {
      case Inr(tail) => tailPrism.getOption(tail)
      case _ => None
    }
    new MkPrismToType(Prism(getOption, reverseGet))
  }
}

trait AutoPrismLP3 extends AutoPrismLP2 {
  implicit def deriveHead[H, T <: Coproduct, A](
    implicit
    ll: Lazy[MkPrismToType[H, A]]
  ): MkPrismToType[H :+: T, A] = {
    val headPrism = ll.value()
    def reverseGet(a: A): H :+: T = Inl[H, T](headPrism.reverseGet(a))
    def getOption(h: H :+: T) = h match {
      case Inl(head) => headPrism.getOption(head)
      case _ => None
    }
    new MkPrismToType(Prism(getOption, reverseGet))
  }
}

object MkPrismToType extends AutoPrismLP3 {
  implicit def subtyping[A, B <: A](implicit
    notCoproduct: B <:!< Coproduct,
    typeable: Typeable[B]
  ): MkPrismToType[A, B] = {
    val _ = notCoproduct
    new MkPrismToType(Prism(typeable.cast(_), identity))
  }
}
