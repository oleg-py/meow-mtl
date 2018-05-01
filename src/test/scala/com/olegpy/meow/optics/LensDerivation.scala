package com.olegpy.meow.optics

import minitest._

object LensDerivation extends SimpleTestSuite {
  case class DbConfig(jdbcUrl: JDBCUrl, poolSize: PoolSize)
  case class JDBCUrl(s: String) extends AnyVal
  case class PoolSize(n: Int) extends AnyVal

  case class HttpConfig(host: Host, port: Port)
  case class Port(n: Int) extends AnyVal
  case class Host(s: String) extends AnyVal

  case class AppConfig(http: HttpConfig, db: DbConfig)

  val config = AppConfig(
    HttpConfig(Host("localhost"), Port(80)),
    DbConfig(JDBCUrl("db:mem"), PoolSize(10))
  )

  private def lenz[A, B](implicit mk: MkLensToType[A, B]) = mk()

  test("derived for root") {
    assertEquals(lenz[AppConfig, AppConfig].get(config), config)
  }

  test("derived for first level") {
    assertEquals(lenz[AppConfig, HttpConfig].get(config), config.http)
    assertEquals(lenz[AppConfig, DbConfig].get(config), config.db)
    assertEquals(lenz[DbConfig, PoolSize].get(config.db), config.db.poolSize)
  }

  test("derived for 2nd level") {
    assertEquals(lenz[AppConfig, Port].get(config), config.http.port)
    assertEquals(lenz[AppConfig, JDBCUrl].get(config), config.db.jdbcUrl)
  }

  test("derives ambiguous instances in depth-first fashion") {
    assertEquals(lenz[AppConfig, Int].get(config), config.http.port.n)
    assertEquals(lenz[AppConfig, String].get(config), config.http.host.s)
  }
}
