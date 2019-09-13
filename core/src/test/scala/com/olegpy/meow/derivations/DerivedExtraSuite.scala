package com.olegpy.meow.derivations

import cats.mtl.ApplicativeHandle
import cats.mtl.instances.all._
import cats.instances.all._
import cats.syntax.either._
import com.olegpy.meow.hierarchy._
import minitest.SimpleTestSuite


object DerivedExtraSuite extends SimpleTestSuite {

  type Data = Either[String, Int]

  test("ApplicativeHandle.handle totality") {
    type M[A] = Either[Data, A]

    def forStr[F[_] : ApplicativeHandle[*[_], Data]]: ApplicativeHandle[F, String] = implicitly
    def forInt[F[_] : ApplicativeHandle[*[_], Data]]: ApplicativeHandle[F, Int] = implicitly

    assert(forStr[M].handle(forInt[M].raise(42))(identity) === 42.asRight.asLeft)
  }
}
