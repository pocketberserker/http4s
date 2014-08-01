package org.http4s

import scala.collection.JavaConverters._
import java.nio.charset.{Charset => NioCharset}
import org.scalacheck.Gen
import org.scalacheck.Gen._

trait Generators {
  lazy val nioCharsets: Gen[NioCharset] = oneOf(NioCharset.availableCharsets.values.asScala.toSeq)
  lazy val charsets: Gen[Charset] = nioCharsets.map(Charset.fromNioCharset)

  lazy val atomicCharsetRanges: Gen[CharsetRange.Atom] = for {
    charset <- charsets
    q <- qualities
  } yield charset.withQuality(q)

  lazy val anyCharsetRanges: Gen[CharsetRange] = qualities.map(CharsetRange.`*`.withQuality)
  lazy val charsetRanges: Gen[CharsetRange] = oneOf(atomicCharsetRanges, anyCharsetRanges)

  lazy val qualities: Gen[Q] = Gen.choose(0, 1000).map(Q.fromInt)
}
