package org.http4s

import java.io._

import xml.{Elem, XML}
import org.xml.sax.InputSource
import javax.xml.parsers.SAXParser
import scalaz.stream.{processes, process1}
import scalaz.concurrent.Task
import scalaz.stream.Process._
import scala.util.control.NoStackTrace
import scodec.bits.ByteVector

object EntityBody extends EntityBodyFunctions {
  val empty: EntityBody = halt
  val DefaultMaxEntitySize: Int = Http4sConfig.getInt("org.http4s.default-max-entity-size")
}

trait EntityBodyFunctions {
  def text: Message => Task[String] = text(EntityBody.DefaultMaxEntitySize)

  def text(limit: Int)(msg: Message): Task[String] = {
    val buff = new StringBuilder
    (msg.body |> takeBytes(limit) |> processes.fold(buff) { (b, c) => {
      b.append(new String(c.toArray, (msg.charset.charset)))
    }}).map(_.result()).runLastOr("")
  }

  /**
   * Handles a message body as XML.
   *
   * TODO Not an ideal implementation.  Would be much better with an asynchronous XML parser, such as Aalto.
   *
   * @param limit the maximum size before an EntityTooLarge error is returned
   * @param parser the SAX parser to use to parse the XML
   * @return an XML element
   */
  def xml(limit: Int, parser: SAXParser)(msg: Message): Task[Elem] =
    text(limit)(msg).map { s =>
    // TODO: exceptions here should be handled by Task, but are not until 7.0.5+
      val source = new InputSource(new StringReader(s))
      XML.loadXML(source, parser)
    }

  def xml: Message => Task[Elem] = xml(EntityBody.DefaultMaxEntitySize, XML.parser)

  private def takeBytes(n: Int): Process1[ByteVector, ByteVector] = {
    def go(taken: Int, chunk: ByteVector): Process1[ByteVector, ByteVector] = {
      val sz = taken + chunk.length
      if (sz > n) fail(EntityTooLarge(n))
      else Emit(Seq(chunk), await(Get[ByteVector])(go(sz, _)))
    }
    await(Get[ByteVector])(go(0,_))
  }

  def comsumeUpTo(n: Int): Process1[ByteVector, ByteVector] = {
    val p = process1.fold[ByteVector, ByteVector](ByteVector.empty) { (c1, c2) => c1 ++ c2 }
    takeBytes(n) |> p
  }

  // File operations
  // TODO: rewrite these using NIO non blocking FileChannels
  def binFile(msg: Message, file: java.io.File)(f: => Task[Response]) = {
    val out = new java.io.FileOutputStream(file)
    msg.body
      .map{c => out.write(c.toArray) }
      .run.flatMap{_ => out.close(); f}
  }

  def textFile(msg: Message, in: java.io.File)(f: => Task[Response]): Task[Response] = {
    val is = new java.io.PrintStream(new FileOutputStream(in))
    msg.body
      .map{ d => is.print(new String(d.toArray, msg.charset.charset)) }
      .run.flatMap{_ => is.close(); f}
  }
}

case class EntityTooLarge(limit: Int) extends Exception with NoStackTrace
