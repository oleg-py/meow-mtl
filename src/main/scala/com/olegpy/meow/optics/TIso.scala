package com.olegpy.meow.optics

abstract class TIso[S, A] {
  def from(a: S): A
  def to(b: A): S

  def toPrism: TPrism[S, A] = new TPrism[S, A] {
    def apply(a: A): S = to(a)
    def unapply(s: S): Option[A] = Some(from(s))
  }

  def toLens: TLens[S, A] = new TLens[S, A] {
    def get(s: S): A = from(s)
    def set(s: S)(a: A): S = to(a)
  }
}
