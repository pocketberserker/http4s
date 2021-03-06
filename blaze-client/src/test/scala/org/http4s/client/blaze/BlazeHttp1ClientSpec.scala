package org.http4s.client.blaze

import org.http4s.Method._
import org.http4s._
import org.http4s.client.ClientSyntax
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class BlazeHttp1ClientSpec extends Specification with NoTimeConversions {

  "Blaze Simple Http1 Client" should {
    implicit def client = SimpleHttp1Client

    "Make simple http requests" in {
      val resp = Get("http://www.google.com/").on(Status.Ok)(EntityDecoder.text).run
//      println(resp.copy(body = halt))

      resp.status.code must be_==(200)
    }

    "Make simple https requests" in {
      val resp = Get("https://www.google.com/").on(Status.Ok)(EntityDecoder.text).run
//      println(resp.copy(body = halt))
//      println("Body -------------------------\n" + gatherBody(resp.body) + "\n--------------------------")
      resp.status.code must be_==(200)
    }
  }

  sequential

  "RecyclingHttp1Client" should {
    implicit val client = new PooledHttp1Client()

    "Make simple http requests" in {
      val resp = Get("http://www.google.com/").on(Status.Ok)(EntityDecoder.text).run
      //      println(resp.copy(body = halt))

      resp.status.code must be_==(200)
    }

    "Repeat a simple http request" in {
      val f = 0 until 10 map { _ =>
        Future {
          val resp = Get("http://www.google.com/").on(Status.Ok)(EntityDecoder.text).run
          //      println(resp.copy(body = halt))

          resp.status.code must be_==(200)
        }
      } reduce((f1, f2) => f1.flatMap(_ => f2))

      Await.result(f, 10.seconds)
    }

    "Make simple https requests" in {
      val resp = Get("https://www.google.com/").on(Status.Ok)(EntityDecoder.text).run
      //      println(resp.copy(body = halt))
      //      println("Body -------------------------\n" + gatherBody(resp.body) + "\n--------------------------")
      resp.status.code must be_==(200)
    }

    "Shutdown the client" in {
      client.shutdown().run
      true must be_==(true)
    }
  }
}
