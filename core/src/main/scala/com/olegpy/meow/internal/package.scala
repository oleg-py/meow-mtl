package com.olegpy.meow

import scala.reflect.ClassTag

import shapeless.Refute


package object internal {
  type IsAbstract[F[_]] = Refute[ClassTag[F[Any]]]
}
