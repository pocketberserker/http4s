package org.http4s

import org.http4s.CharsetRange.`*`
import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class CharsetRangeSpec extends Specification with ScalaCheck with Generators {
  "*" should {
    "be satisfied by any charset when q > 0" in {
      Prop.forAll(anyCharsetRanges suchThat (_.q.doubleValue != Q.MIN_VALUE), charsets) { (range, cs) =>
        range isSatisfiedBy cs must beTrue
      }
    }

    "not be satisfied when q = 0" in {
      Prop.forAll(charsets) { cs =>
        `*`.withQuality(Q.fromDouble(0.0)) isSatisfiedBy cs must beFalse
      }
    }
  }

  "atomic charset ranges" should {
    "be satisfied by themselves if q > 0" in {
      Prop.forAll(atomicCharsetRanges suchThat (_.q.doubleValue != Q.MIN_VALUE)) { range =>
        range isSatisfiedBy range.charset must beTrue
      }
    }

    "not be satisfied by any other charsets" in {
      Prop.forAll(atomicCharsetRanges, charsets) { (range, cs) =>
        range.charset != cs ==> { range isSatisfiedBy cs must beFalse }
      }
    }
  }

  "withQuality" should {
    "reject q < 0" in {
      Prop.forAll(charsetRanges, Gen.negNum[Double]) { (range, q) =>
        range.withQuality(Q.fromDouble(q)) must throwAn [IllegalArgumentException]
      }
    }

    "reject q > 1" in {
      Prop.forAll(charsetRanges, Gen.posNum[Double] suchThat (_ > 1.0)) { (range, q) =>
        range.withQuality(Q.fromDouble(q)) must throwAn [IllegalArgumentException]
      }
    }
  }

  "toString" should {
    "start with the charset name for atomic charsets" in {
      Prop.forAll(atomicCharsetRanges) { cs =>
        cs.toString must startWith(cs.charset.nioCharset.name)
      }
    }

    "start with '*' for the wild card charset" in {
      Prop.forAll(anyCharsetRanges) { cs =>
        cs.toString must startWith("*")
      }
    }

    "include the q-value when < 1" in {
      Prop.forAll(charsetRanges suchThat (_.q.doubleValue < 1.0)) { cs =>
        cs.toString must contain(";q=")
      }
    }
  }
}
