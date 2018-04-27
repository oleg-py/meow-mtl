package com.olegpy.meow

package object optics {
  type TLens[S, A] = shapeless.Lens[S, A]
}
