/*
* Derived from Twitter Finagle.
*
* Original source:
* https://github.com/twitter/finagle/blob/6e2462acc32ac753bf4e9d8e672f9f361be6b2da/finagle-http/src/test/scala/com/twitter/finagle/http/path/PathSpec.scala
*/
package org.http4s
package dsl

import org.specs2.mutable.Specification

class PathSpec extends Specification {
  "Path" should {

    "/foo/bar" in {
      Path("/foo/bar").toList must_== (List("foo", "bar"))
    }

    "foo/bar" in {
      Path("foo/bar").toList must_== (List("foo", "bar"))
    }


    "~ extractor on Path" in {
      (Path("/foo.json") match {
        case Root / "foo" ~ "json" => true
        case _                     => false
      }) must beTrue
    }

    "~ extractor on filename foo.json" in {
      ("foo.json" match {
        case "foo" ~ "json" => true
        case _              => false
      }) must beTrue
    }

    "~ extractor on filename foo" in {
      ("foo" match {
        case "foo" ~ "" => true
        case _          => false
      }) must beTrue
    }

    "-> extractor /test.json" in {
      val req = Request(requestMethod = Method.Get, requestUri = Uri.fromString("/test.json").get)
      (req match {
        case GET -> Root / "test.json" => true
        case _                         => false
      }) must beTrue
    }

    "-> extractor /foo/test.json" in {
      val req = Request(requestMethod = Method.Get, requestUri = Uri.fromString("/foo/test.json").get)
      (req match {
        case GET -> Root / "foo" / "test.json" => true
        case _                         => false
      }) must beTrue
    }

   "request path info extractor for /" in {
      val req = Request(requestMethod = Method.Get, requestUri = Uri.fromString("/").get)
      (req match {
        case _ -> Root => true
        case _ => false
      }) must beTrue
    }

    "Root extractor" in {
      (Path("/") match {
        case Root => true
        case _    => false
      }) must beTrue
    }

    "Root extractor, no partial match" in {
      (Path("/test.json") match {
        case Root => true
        case _    => false
      }) must beFalse
    }

    "Root extractor, empty path" in {
      (Path("") match {
        case Root => true
        case _    => false
      }) must beTrue
    }

    "/ extractor" in {
      (Path("/1/2/3/test.json") match {
        case Root / "1" / "2" / "3" / "test.json" => true
        case _                                    => false
      }) must beTrue
    }

    "Int extractor" in {
      (Path("/user/123") match {
        case Root / "user" / IntVar(userId) => userId == 123
        case _                                => false
      }) must beTrue
    }

    "Int extractor, invalid int" in {
      (Path("/user/invalid") match {
        case Root / "user" / IntVar(userId) => true
        case _                                => false
      }) must beFalse
    }

    "Int extractor, number format error" in {
      (Path("/user/2147483648") match {
        case Root / "user" / IntVar(userId) => true
        case _                                => false
      }) must beFalse
    }

    "Long extractor" in {
      (Path("/user/123") match {
        case Root / "user" / LongVar(userId) => userId == 123
        case _                                 => false
      }) must beTrue
    }

    "Long extractor, invalid int" in {
      (Path("/user/invalid") match {
        case Root / "user" / LongVar(userId) => true
        case _                                 => false
      }) must beFalse
    }

    "Long extractor, number format error" in {
      (Path("/user/9223372036854775808") match {
        case Root / "user" / LongVar(userId) => true
        case _                                 => false
      }) must beFalse
    }
  }
}
