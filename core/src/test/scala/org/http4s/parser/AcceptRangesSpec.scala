package org.http4s.parser

import org.scalatest.{Matchers, WordSpec}
import org.http4s.Header.`Accept-Ranges`
import scalaz.Validation
import org.http4s.RangeUnit

class AcceptRangesSpec  extends WordSpec with Matchers with HeaderParserHelper[`Accept-Ranges`] {

  def hparse(value: String) = HttpParser.ACCEPT_RANGES(value)

  "Accept-Ranges header" should {

    val ranges = List(`Accept-Ranges`.bytes,
                      `Accept-Ranges`.none,
                      `Accept-Ranges`(RangeUnit.CustomRangeUnit("foo")),
                      `Accept-Ranges`(RangeUnit.bytes, RangeUnit.CustomRangeUnit("bar")))

    "Give correct header value" in {
      ranges.map(_.value) should equal (List("bytes", "none", "foo", "bytes, bar"))
    }

//    "Do whitespace right" in {
//      val value = " bytes"
//      parse(value) should equal(`Accept-Ranges`.bytes)
//    }

    "Parse correctly" in {
      ranges.foreach { r =>
        parse(r.value) should equal(r)
      }
    }

  }

}
