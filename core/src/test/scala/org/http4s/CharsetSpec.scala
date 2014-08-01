package org.http4s

import java.util.Locale

import org.http4s.util.string._
import java.nio.charset.{Charset => NioCharset}

import org.scalacheck.{Prop, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import scala.collection.JavaConverters._
import scalaz.syntax.id._

class CharsetSpec extends Specification with ScalaCheck with Generators {
  "Charset.fromString" should {
    "be case insensitive" in {
      Prop.forAll(nioCharsets) { cs: NioCharset =>
        val upper = cs.name.toUpperCase(Locale.ROOT)
        val lower = cs.name.toLowerCase(Locale.ROOT)
        Charset.fromString(upper) must_== Charset.fromString(lower)
      }
    }

    "work for aliases" in {
      Charset.fromString("UTF8") must_== Charset.`UTF-8`.right
    }

    "return InvalidCharset for unregistered names" in {
      Charset.fromString("blah") must_== InvalidCharset("blah").left
    }
  }

  "toString" should {
    "be the charset name" in {
      Prop.forAll(charsets) { cs: Charset =>
        cs.toString == cs.nioCharset.name
      }
    }
  }
}
