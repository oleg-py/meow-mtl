package com.olegpy.meow.optics

import minitest._
import shapeless.test.illTyped

object PrismDerivation extends SimpleTestSuite {
  sealed trait ADT
  case object Value extends ADT
  case class Nested(inner: Either[String, Int]) extends ADT
  case class Plain(n: Long) extends ADT

  def prizm[A, B](implicit mk: MkPrismToType[A, B]) = mk()

  test("Derives for root elements") {
    assert(prizm[ADT, ADT].unapply(Plain(0)) contains Plain(0))
  }

  test("Derives for first level") {
    assert(prizm[ADT, Value.type].unapply(Value) contains Value)
  }

  test("Can go inside values") {
    assert(prizm[ADT, Long].unapply(Plain(42)) contains 42)
    illTyped("""prizm[ADT, String]""") // TODO
  }
}
