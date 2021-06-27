package com.olegpy.meow.internal

import cats.Id
import cats.mtl.Ask
import com.olegpy.meow.optics.MkLensToType

import java.util.concurrent.atomic.AtomicLong
import scala.reflect.macros.whitebox
import scala.reflect.macros.runtime
import scala.language.experimental.macros

object Macros {

  def deriveAsk[F[_], A](c: whitebox.Context): c.Expr[Ask[F, A]] = {
    import c.universe._

    if (c.enclosingMacros.size > 1) abortExpansion("unsupported recursive call")

    // yes, we're importing compiler internals
    val cc = c.asInstanceOf[runtime.Context]
    val global: cc.universe.type = cc.universe
    val analyzer: global.analyzer.type = global.analyzer

    val requestedType = c.openImplicits.head.pt.asInstanceOf[global.Type]

    val actualF = requestedType.typeArgs(0)
    val actualE = requestedType.typeArgs(1)

    val typer = cc.callsiteTyper

    val ask = requestedType.typeConstructor
    val askF = ask.typeParams(0)
    val askE = ask.typeParams(1)

    val lensTypeConstructor = implicitly[c.WeakTypeTag[MkLensToType[Any, Any]]].tpe.typeConstructor.asInstanceOf[global.Type]
    val lensS = lensTypeConstructor.typeParams(0)
    val lensA = lensTypeConstructor.typeParams(1)

    val ctx = typer.context.makeImplicit(false)
    val potentialParentType = ask.instantiateTypeParams(List(askF, askE), List(global.WildcardType, global.WildcardType))
    val position = c.enclosingPosition.asInstanceOf[global.Position]

    val search = new analyzer.ImplicitSearch(cc.macroApplication, potentialParentType, false, ctx, position)
    val potentialParents = search.allImplicits

    var foundLens: Option[Tree] = None

    def isParent(sr: analyzer.SearchResult): Boolean = {
      if (sr.isFailure) return false
      val typ = sr.tree.tpe
      if (typ.typeArgs.head.normalize != actualF.normalize) return false

      val stateType = typ.typeArgs(1)
      if (stateType.normalize <:< actualE.normalize) abortExpansion("covered by subtyping")
      if (!canBeFocused(stateType)) return false

      true
    }

    def canBeFocused(stateType: global.Type): Boolean = {
      val lensType = lensTypeConstructor.instantiateTypeParams(List(lensS, lensA), List(stateType, actualE)).asInstanceOf[c.Type]

      val inferredLens = c.inferImplicitValue(lensType)
      if (inferredLens.isEmpty) return false

      foundLens = Some(inferredLens)

      true
    }

    // TODO: this is technically wrong, because if we found more than one we should emit ambiguous implicit error or sth... maybe?
    //  I don't know if we would have to replicate the logic that the compiler uses for prioritization and disambiguation
    // IDEA: maybe extend ImplicitSearch and change its behaviour so it performs our checks and then just use `ImplicitSearch#bestImplicit`
    val parent = potentialParents find isParent

    if (parent.isEmpty) abortExpansion("did not find a suitable parent type")

    val parentTree = parent.get.tree.asInstanceOf[c.Tree]
    val parentE = parentTree.tpe.typeArgs(1).asInstanceOf[global.Type]
    val lens = foundLens.get
    val askImplCons = implicitly[cc.WeakTypeTag[AskOptics.Applicative[Id, Unit, Unit]]].tpe.typeConstructor
    val askImpl = askImplCons
      .instantiateTypeParams(askImplCons.typeParams, List(actualF, parentE, actualE))
      .asInstanceOf[c.Type]

    import scala.util.Try
    if (Try(System.getProperty("meow-mtl.loud-ask-derivation").toBoolean).getOrElse(false)) {
      // the counter is so the compiler doesn't dedup the second message, because that's confusing
      val invocationNum = invocationCounter.getAndIncrement()
      c.info(c.enclosingPosition, s"Deriving Ask from parent instance ($invocationNum)...", true)
      c.info(parentTree.symbol.pos, s"... parent instance ($invocationNum) located here", true)
    }

    c.Expr[Ask[F, A]](q"""
      new $askImpl($parentTree, $lens())
    """)
  }

  val invocationCounter = new AtomicLong(0)

  // I don't think this is ever seen by the user when compiling, just here for code clarity...
  private def abortExpansion(message: String = "<no message>"): Nothing = throw new RuntimeException(s"aborting macro expansion: $message")

}
