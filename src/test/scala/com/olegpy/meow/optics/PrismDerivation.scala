package com.olegpy.meow.optics

import minitest._

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
    val nested = Nested(Left("Foo"))
    assert(prizm[ADT, Nested].unapply(nested) contains nested)
  }

  test("Can go inside values") {
    assert(prizm[ADT, Long].unapply(Plain(42)) contains 42)
    assert(prizm[ADT, String].unapply(Nested(Left("Foo"))) contains "Foo")
  }
}
