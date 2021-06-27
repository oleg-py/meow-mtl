package com.olegpy.meow.internal

import cats.Id
import cats.mtl.Ask
import com.olegpy.meow.internal.AskOptics.Invariant

import scala.reflect.macros.whitebox
import scala.language.experimental.macros

object Macros {
  def deriveAsk[F[_], A](c: whitebox.Context): c.Expr[Ask[F, A]] = {
    import c.universe._

    if (c.enclosingMacros.size > 1) abortExpansion("unsupported recursive call")

    val ask = c.openImplicits.head.pt
    val invariantAsk = makeInvariant(c)(ask)

    val foundImplicit = c.inferImplicitValue(invariantAsk)
    if (foundImplicit.isEmpty) abortExpansion("found no suitable implicits")

    val invariantObject = implicitly[c.WeakTypeTag[Invariant.type]].tpe.typeSymbol
    val convert = invariantObject.info.decls.find(_.name.toString == "convert")
    if (convert.contains(foundImplicit.symbol)) abortExpansion("trivial wrap - ambiguities")

    c.Expr[Ask[F, A]](q"$foundImplicit.value")
  }

  def makeInvariant(c: whitebox.Context)(ask: c.Type): c.Type = {
    import c.universe._

    val invAsk = implicitly[c.WeakTypeTag[AskOptics.Invariant[Id, Any]]].tpe.typeConstructor
    val args = ask.typeArgs

    appliedType(invAsk, args)
  }


  // I don't think this is ever seen by the user when compiling, just here for code clarity...
  private def abortExpansion(message: String = "<no message>"): Nothing = throw new RuntimeException(s"aborting macro expansion: $message")
}
