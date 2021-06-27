package com.olegpy.meow.internal

import cats.Id
import cats.mtl.Ask
import com.olegpy.meow.internal.AskOptics.Invariant

import scala.reflect.macros.whitebox
import scala.reflect.macros.runtime
import scala.language.experimental.macros

object Macros {
  def deriveAsk[F[_], A](c: whitebox.Context): c.Expr[Ask[F, A]] = {
    import c.universe._

    if (c.enclosingMacros.size > 1) abortExpansion("unsupported recursive call")

    // we need this just for `instantiateTypeParams` :/
    val compInternals = c.asInstanceOf[runtime.Context]

    val ask = c.openImplicits.head.pt.asInstanceOf[compInternals.Type]
    val invAsk = implicitly[c.WeakTypeTag[AskOptics.Invariant[Id, Any]]].tpe.typeConstructor.asInstanceOf[compInternals.Type]
    val invAskApl = invAsk.instantiateTypeParams(invAsk.typeParams, ask.typeArgs).asInstanceOf[c.Type]

    val foundImplicit = c.inferImplicitValue(invAskApl)

    val invariantObject = implicitly[c.WeakTypeTag[Invariant.type]].tpe.typeSymbol
    val convert = invariantObject.info.decls.find(_.name.toString == "convert")
    if (convert.contains(foundImplicit.symbol)) abortExpansion("trivial wrap - ambiguities")
    if (foundImplicit.isEmpty) abortExpansion("found no suitable implicits")

    c.Expr[Ask[F, A]](q"$foundImplicit.value")
  }


  // I don't think this is ever seen by the user when compiling, just here for code clarity...
  private def abortExpansion(message: String = "<no message>"): Nothing = throw new RuntimeException(s"aborting macro expansion: $message")
}
