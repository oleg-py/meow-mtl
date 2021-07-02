package com.olegpy.meow.internal

import cats.mtl.Tell
import com.olegpy.meow.optics.TPrism


private[meow] object TellOptics {
  class Functor[F[_], S, A](
    parent: Tell[F, S],
    prism: TPrism[S, A]
  ) extends Tell[F, A] {
    val functor: cats.Functor[F] = parent.functor
    def tell(l: A): F[Unit] = parent.tell(prism(l))
  }
}
