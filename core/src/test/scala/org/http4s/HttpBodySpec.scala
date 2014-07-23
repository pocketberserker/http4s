package org.http4s

import scala.language.postfixOps
import org.xml.sax.SAXParseException
import scodec.bits.ByteVector

import scalaz.concurrent.Task

// the http4s team resents importing this.

import Status._

import java.io.{FileInputStream,File,InputStreamReader}
import org.scalatest.{Matchers, WordSpec}

import scalaz.stream.Process._
import HttpBody._


class HttpBodySpec extends WordSpec with Matchers {

  def getBody(body: HttpBody): Array[Byte] = body.runLog.run.reduce(_ ++ _).toArray

  "xml" should {

    val server: Request => Task[Response] = { req =>
      xml(req).flatMap{ elem => Ok(elem.label) }
                    .handle{ case t: SAXParseException => Status.BadRequest().run }
    }

    "parse the XML" in {
      val resp = server(Request(body = emit("<html><h1>h1</h1></html>").map(s => ByteVector(s.getBytes)))).run
      resp.status should equal(Status.Ok)
      getBody(resp.body) should equal ("html".getBytes)
    }

    "handle a parse failure" in {
      val body = emit("This is not XML.").map(s => ByteVector(s.getBytes))
      val resp = server(Request(body = body)).run
      resp.status should equal (Status.BadRequest)
    }
  }

  "A File BodyParser" should {
    val binData: Array[Byte] = "Bytes 10111".getBytes

    def readFile(in: File): Array[Byte] = {
      val os = new FileInputStream(in)
      val data = new Array[Byte](in.length.asInstanceOf[Int])
      os.read(data)
      data
    }

    def readTextFile(in: File): String = {
      val os = new InputStreamReader(new FileInputStream(in))
      val data = new Array[Char](in.length.asInstanceOf[Int])
      os.read(data,0,in.length.asInstanceOf[Int])
      data.foldLeft("")(_ + _)
    }

    def mocServe(req: Request)(route: Request => Task[Response]) = {
      route(req.copy(body = emit(binData).map(ByteVector(_))))
    }

    "Write a text file from a byte string" in {
      val tmpFile = File.createTempFile("foo","bar")
      val response = mocServe(Request()) {
        case req =>
          textFile(req, tmpFile){
            Ok("Hello")
          }
      }.run

      readTextFile(tmpFile) should equal (new String(binData))
      response.status should equal (Status.Ok)
      getBody(response.body) should equal ("Hello".getBytes)
    }

    "Write a binary file from a byte string" in {
      val tmpFile = File.createTempFile("foo","bar")
      val response = mocServe(Request()) {
        case req => binFile(req, tmpFile)(Ok("Hello"))
      }.run

      response.status should equal (Status.Ok)
      getBody(response.body) should equal ("Hello".getBytes)
      readFile(tmpFile) should equal (binData)
    }
  }

}

