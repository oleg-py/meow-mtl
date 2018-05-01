package com.olegpy.meow.optics

import minitest._
import shapeless.test.illTyped

object IsoDerivation extends SimpleTestSuite {
  case class Foo()
  case class Bar(s: String)
  case class Num(n: Int) extends AnyVal

  def izo[A, B](implicit mk: MkIsoToType[A, B]) = mk()

  test("Derives for self-type") {
    assertEquals(izo[Foo, Foo].from(Foo()), Foo())
    assertEquals(izo[Foo, Foo].to(Foo()), Foo())
  }

  test("Derives from AnyVal classes") {
    assertEquals(izo[Num, Int].to(42), Num(42))
    assertEquals(izo[Num, Int].from(Num(3)), 3)
  }

  test("Derives from 1-element classes") {
    assertEquals(izo[Bar, String].to("F"), Bar("F"))
    assertEquals(izo[Bar, String].from(Bar("AA")), "AA")
  }

  test("Doesn't derive for 1-element classes in reverse") {
    illTyped("izo[String, Bar]")
    illTyped("izo[Int, Num]")
  }
}
