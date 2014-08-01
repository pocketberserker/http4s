package org.http4s

import util._

sealed abstract class CharsetRange extends QualityFactor with Renderable {
  def q: Q
  def withQuality(q: Q): CharsetRange
  def isSatisfiedBy(charset: Charset): Boolean
  def preferredCharset: Charset
}

object CharsetRange {
  sealed case class `*`(q: Q) extends CharsetRange {
    final override def withQuality(q: Q): CharsetRange = copy(q = q)
    final def isSatisfiedBy(charset: Charset): Boolean = !q.unacceptable
    final def render[W <: Writer](writer: W): writer.type = writer ~ "*" ~ q
    final val preferredCharset = Charset.`UTF-8` // In questionable taste, but makes preferred work
  }

  object `*` extends `*`(Q.Unity)

  final case class Atom protected[http4s] (charset: Charset, q: Q = Q.Unity) extends CharsetRange {
    override def withQuality(q: Q): CharsetRange = copy(q = q)
    def isSatisfiedBy(charset: Charset): Boolean = !q.unacceptable && this.charset == charset
    def render[W <: Writer](writer: W): writer.type = writer ~ charset ~ q
    val preferredCharset = charset
  }

  implicit val characterSetOrdering = new Ordering[CharsetRange] {
    def compare(x: CharsetRange, y: CharsetRange): Int = {
      implicitly[Ordering[Q]].compare(y.q, x.q)
    }
  }

  // TODO No me gusta nada
  implicit def apply(cs: Charset): CharsetRange.Atom = cs.toRange
}
