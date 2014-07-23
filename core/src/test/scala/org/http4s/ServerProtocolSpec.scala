package org.http4s

import org.scalatest.{Matchers, WordSpec}
import org.http4s.util.string._

class ServerProtocolSpec extends WordSpec with Matchers {
  import ServerProtocol._

  def resolve(str: String) = ServerProtocol.getOrElseCreate(str.ci)

  "HTTP versions" should {
    "be registered if standard" in {
      resolve("HTTP/1.1") should be theSameInstanceAs `HTTP/1.1`
    }

    "parse for future versions" in {
      resolve("HTTP/1.3") should equal(HttpVersion(1, 3))
    }

    "render with protocol and version" in {
      `HTTP/1.0`.toString should equal ("HTTP/1.0")
    }
  }

  "INCLUDED" should {
    "be registered" in {
      resolve("INCLUDED") should be theSameInstanceAs INCLUDED
    }

    "render as 'INCLUDED'" in {
      INCLUDED.toString should equal ("INCLUDED")
    }
  }

  "Extension versions" should {
    "parse with a version" in {
      resolve("FOO/2.10") should equal (ExtensionVersion("FOO".ci, Some(Version(2, 10))))
    }

    "parse without a version" in {
      resolve("FOO") should equal (ExtensionVersion("FOO".ci, None))
    }

    "render with a version" in {
      resolve("FOO/2.10").toString should equal ("FOO/2.10")
    }

    "render without a verison" in {
      resolve("FOO").toString should equal ("FOO")
    }
  }

  "HttpVersion extractor" should {
    "recognize HttpVersions" in {
      HttpVersion.unapply(`HTTP/1.1`: ServerProtocol) should equal (Some(1 -> 1))
    }

    "treat INCLUDED as HTTP/1.0" in {
      HttpVersion.unapply(INCLUDED) should equal (Some(1 -> 0))
    }

    "treat extension versions as not HTTP" in {
      HttpVersion.unapply(ExtensionVersion("Foo".ci, Some(Version(1, 1)))) should be (None)
    }
  }
}
