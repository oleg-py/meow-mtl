package com.olegpy.meow

import shapeless.=:!=


package object internal {
  type Not[A] = { type l[B] = A =:!= B }
}
