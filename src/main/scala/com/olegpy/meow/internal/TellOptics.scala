package com.olegpy.meow.internal

import cats.mtl.{DefaultFunctorTell, FunctorTell}
import com.olegpy.meow.optics.TPrism


private[meow] object TellOptics {
  class Functor[F[_], S, A](
    parent: FunctorTell[F, S],
    prism: TPrism[S, A]
  ) extends FunctorTell[F, A] with DefaultFunctorTell[F, A] {
    val functor: cats.Functor[F] = parent.functor
    def tell(l: A): F[Unit] = parent.tell(prism(l))
  }
}
