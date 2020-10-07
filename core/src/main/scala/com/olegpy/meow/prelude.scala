package com.olegpy.meow

import com.olegpy.meow.internal.ParentInstances


/**
 * Import this to have low-priority derivations
 * for base classes of MTL typeclasses
 * (e.g. Stateful => Monad, Raise => Functor)
 */
object prelude extends ParentInstances
