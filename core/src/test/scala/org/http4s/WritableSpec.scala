package org.http4s

import java.io.{StringReader, ByteArrayInputStream, FileWriter, File}
import java.nio.charset.StandardCharsets

import org.specs2.mutable.Specification

import scala.concurrent.Future
import scalaz.Rope
import scalaz.concurrent.Task
import scalaz.stream.text.utf8Decode
import scalaz.stream.Process

object WritableSpec {
  def writeToString[A](a: A)(implicit W: Writable[A]): String =
    Process.eval(W.toEntity(a))
      .collect { case Writable.Entity(body, _ ) => body }
      .flatMap(identity)
      .fold1Monoid
      .pipe(utf8Decode)
      .runLastOr("")
      .run
}

class WritableSpec extends Specification with Http4s {
  import WritableSpec.writeToString

  "Writable" should {
    "render strings" in {
      writeToString("pong") must_== "pong"
    }

    "calculate the content length of strings" in {
      implicitly[Writable[String]].toEntity("pong").run.length must_== Some(4)
    }

    "render integers" in {
      writeToString(1) must_== "1"
    }

    "render html" in {
      val html = <html><body>Hello</body></html>
      writeToString(html) must_== "<html><body>Hello</body></html>"
    }

    "render byte arrays" in {
      val hello = "hello"
      writeToString(hello.getBytes(StandardCharsets.UTF_8)) must_== hello
    }

    "render futures" in {
      import scala.concurrent.ExecutionContext.Implicits.global
      val hello = "Hello"
      writeToString(Future(hello)) must_== hello
    }

    "render Tasks" in {
      val hello = "Hello"
      writeToString(Task.now(hello)) must_== hello
    }

    "render processes" in {
      val helloWorld = Process("hello", "world")
      writeToString(helloWorld) must_== "helloworld"
    }

    "render files" in {
      val tmpFile = File.createTempFile("http4s-test-", ".txt")
      try {
        val w = new FileWriter(tmpFile)
        try w.write("render files test")
        finally w.close()
        writeToString(tmpFile) must_== "render files test"
      }
      finally tmpFile.delete()
    }

    "render input streams" in {
      val inputStream = new ByteArrayInputStream(("input stream").getBytes(StandardCharsets.UTF_8))
      writeToString(inputStream) must_== "input stream"
    }

    "render readers" in {
      val reader = new StringReader("string reader")
      writeToString(reader) must_== "string reader"
    }

    "render text ropes" in {
      val rope = Rope.fromString("text rope")
      writeToString(rope) must_== "text rope"
    }

    "render binary ropes" in {
      val rope = Rope.fromArray("binary rope".getBytes(StandardCharsets.UTF_8))
      writeToString(rope) must_== "binary rope"
    }
  }
}

