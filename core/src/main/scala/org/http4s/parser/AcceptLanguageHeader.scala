/*
 * Derived from https://github.com/spray/spray/blob/v1.1-M7/spray-http/src/main/scala/spray/http/parser/AcceptLanguageHeader.scala
 *
 * Copyright (C) 2011-2012 spray.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.http4s.parser

import org.parboiled2._

import org.http4s.Header.`Accept-Language`
import org.http4s.{LanguageTag, Q}

private[parser] trait AcceptLanguageHeader {

  def ACCEPT_LANGUAGE(value: String) = new AcceptLanguageParser(value).parse

  private class AcceptLanguageParser(value: String)
    extends Http4sHeaderParser[`Accept-Language`](value) with MediaParser {
    def entry: Rule1[`Accept-Language`] = rule {
      oneOrMore(languageTag).separatedBy(ListSep) ~> { tags: Seq[LanguageTag] =>
        `Accept-Language`(tags.head, tags.tail:_*)
      }
    }

    def languageTag: Rule1[LanguageTag] = rule {
      capture(oneOrMore(Alpha)) ~ zeroOrMore("-" ~ Token) ~ TagQuality ~>
        { (main: String, sub: Seq[String], q: Q) => LanguageTag(main, q, sub) }
    }


    def TagQuality: Rule1[Q] = rule {
      (";" ~ OptWS ~ "q" ~ "=" ~ QValue) | push(Q.Unity)
    }

  }

}
