package com.olegpy.meow.internal

import cats.Id
import cats.mtl.Ask
import cats.mtl.Raise
import cats.mtl.Tell
import com.olegpy.meow.optics.MkLensToType
import com.olegpy.meow.optics.MkPrismToType

import scala.reflect.macros.whitebox
import scala.reflect.macros.runtime
import scala.language.experimental.macros

object Macros {

  def deriveAsk[F[_], E](c: whitebox.Context): c.Expr[Ask[F, E]] = {
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

    val typeclass = requestedType.typeConstructor
    val typeclassF = typeclass.typeParams(0)
    val typeclassE = typeclass.typeParams(1)

    val opticsTypeConstructor =
      if (typeclassE.isCovariant)
        implicitly[c.WeakTypeTag[MkLensToType[Any, Any]]].tpe.typeConstructor.asInstanceOf[global.Type]
      else if (typeclassE.isContravariant)
        implicitly[c.WeakTypeTag[MkPrismToType[Any, Any]]].tpe.typeConstructor.asInstanceOf[global.Type]
      else
        abortExpansion("type classes with invariant second type param are unsupported")

    val opticsS = opticsTypeConstructor.typeParams(0)
    val opticsA = opticsTypeConstructor.typeParams(1)

    val ctx = typer.context.makeImplicit(false)
    val potentialParentType = typeclass.instantiateTypeParams(List(typeclassF, typeclassE), List(global.WildcardType, global.WildcardType))
    val position = c.enclosingPosition.asInstanceOf[global.Position]

    val search = new analyzer.ImplicitSearch(cc.macroApplication, potentialParentType, false, ctx, position)
    val potentialParents = search.allImplicits

    var mkOptic: Option[Tree] = None

    def isParent(sr: analyzer.SearchResult): Boolean = {
      if (sr.isFailure) return false
      val typ = sr.tree.tpe
      if (typ.typeArgs.head.normalize != actualF.normalize) return false

      val stateType = typ.typeArgs(1)
      if (stateType.normalize == actualE.normalize) abortExpansion("better implicit in scope") // otherwise ambiguity in 2.12
      if (!canBeFocused(stateType)) return false

      true
    }

    def canBeFocused(stateType: global.Type): Boolean = {
      val lensType = opticsTypeConstructor.instantiateTypeParams(List(opticsS, opticsA), List(stateType, actualE)).asInstanceOf[c.Type]

      val inferredLens = c.inferImplicitValue(lensType)
      if (inferredLens.isEmpty) return false

      mkOptic = Some(inferredLens)

      true
    }

    // this is technically wrong, because if we found more than one we should emit ambiguous implicit error or sth
    val parent = potentialParents find isParent

    if (parent.isEmpty) abortExpansion("did not find a suitable parent type")

    val parentTree = parent.get.tree.asInstanceOf[c.Tree]
    val parentE = parentTree.tpe.typeArgs(1).asInstanceOf[global.Type]
    val optics = mkOptic.get
    val typeclassImpl = toImpl(cc)(typeclass, actualF, parentE, actualE).asInstanceOf[c.Type]

    val tree = q"""
      new $typeclassImpl($parentTree, $optics())
    """

    c.Expr[Ask[F, E]](tree)
  }

  private def toImpl(c: runtime.Context)(typeclass: c.Type, F: c.Type, E: c.Type, A: c.Type): c.Type = {
    val Ask = implicitly[c.WeakTypeTag[Ask[Id, Unit]]].tpe.typeConstructor
    val Tell = implicitly[c.WeakTypeTag[Tell[Id, Unit]]].tpe.typeConstructor
    val Raise = implicitly[c.WeakTypeTag[Raise[Id, Unit]]].tpe.typeConstructor

    val impl = typeclass match {
      case Ask   => implicitly[c.WeakTypeTag[AskOptics.Applicative[Id, Unit, Unit]]].tpe
      case Tell  => implicitly[c.WeakTypeTag[TellOptics.Functor[Id, Unit, Unit]]].tpe
      case Raise => implicitly[c.WeakTypeTag[RaiseOptics.Functor[Id, Unit, Unit]]].tpe
      case t     => println(s"unsupported typeclass $t"); abortExpansion(s"unsupported typeclass $t")
    }

    val cons = impl.typeConstructor

    cons.instantiateTypeParams(cons.typeParams, List(F, E, A))
  }

  // I don't think this is ever seen by the user when compiling, just here for code clarity...
  private def abortExpansion(message: String = "<no message>"): Nothing = throw new RuntimeException(s"aborting macro expansion: $message")

}
