package com.olegpy.meow.internal

import cats.mtl.Ask
import com.olegpy.meow.optics.MkLensToType

import scala.reflect.macros.whitebox
import scala.language.experimental.macros

object Macros {

  def deriveAsk[F[_], A](c: whitebox.Context)(implicit F: c.WeakTypeTag[F[A]]): c.Expr[Ask[F, A]] = {
    import c.universe._

    if (c.enclosingMacros.size > 1)
      throw new RuntimeException(
        "nope, either we're recursing ourselves into a stack overflow or some other macro is calling us, to which we politely say frig off"
      )

    val lensTypeTag = implicitly[c.WeakTypeTag[MkLensToType[Any, A]]].tpe

    val requestedType = c.openImplicits.head.pt

    val A = requestedType.typeArgs(1)

    // yes, we're importing compiler internals
    import scala.tools.nsc.Global
    val evil = c.universe.asInstanceOf[Global]

    import evil.analyzer.ImplicitSearch
    import evil.analyzer.SearchResult

    val lensS = lensTypeTag.typeConstructor.typeParams(0).asInstanceOf[evil.Symbol]
    val lensA = lensTypeTag.typeConstructor.typeParams(1).asInstanceOf[evil.Symbol]

    val f = c.getClass.getDeclaredField("callsiteTyper")
    f.setAccessible(true)
    val existingSearch = f.get(c).asInstanceOf[ImplicitSearch]

    val ask = existingSearch.pt.typeConstructor
    val askF = ask.typeParams(0)
    val askA = ask.typeParams(1)

    val tree = existingSearch.tree
    val ctx = existingSearch.context0
    val pt = ask.instantiateTypeParams(List(askF, askA), List(evil.WildcardType, evil.WildcardType))
    val pos = existingSearch.pos

    val search = new ImplicitSearch(tree, pt, false, ctx, pos)
    val potentialWinners = search.allImplicits

    var mkLens: Option[Tree] = None

    def isWinner(sr: SearchResult): Boolean = {
      if (sr.isFailure) return false
      val typ = sr.tree.tpe
      if (typ.typeArgs.head.normalize != F.tpe.normalize) return false

      val stateType = typ.typeArgs(1)
      if (!hasSomewhereInside(stateType, A)) return false

      true
    }

    def hasSomewhereInside(stateType: evil.Type, elementType: c.Type): Boolean = {
      val lens = lensTypeTag
        .typeConstructor
        .asInstanceOf[evil.Type]
        .instantiateTypeParams(List(lensS, lensA), List(stateType, elementType.asInstanceOf[evil.Type]))

      val inferredLens = evil.analyzer.inferImplicitByTypeSilent(lens, ctx, pos)
      if (inferredLens.isFailure) return false

      mkLens = Some(inferredLens.tree.asInstanceOf[c.Tree])

      true
    }

    val parent = potentialWinners find isWinner

    if (parent.isEmpty) throw new RuntimeException("did not find a suitable parent type")

    val p = parent.get.tree.asInstanceOf[c.Tree]
    val l = mkLens.get

    c.Expr[Ask[F, A]](q"""
      new _root_.com.olegpy.meow.internal.AskOptics.Applicative($p, $l())
    """)
  }

}
