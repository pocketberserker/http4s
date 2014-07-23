package org.http4s.blaze.server

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import org.http4s.blaze._
import org.http4s.blaze.pipeline.{Command => Cmd}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Await
import scala.concurrent.duration._


class Http4sHttp1ServerStageSpec extends WordSpec with Matchers {

  def makeString(b: ByteBuffer): String = {
    val p = b.position()
    val a = new Array[Byte](b.remaining())
    b.get(a).position(p)
    new String(a)
  }

  def runRequest(req: Seq[String]): ByteBuffer = {
    val head = new SeqTestHead(req.map(s => ByteBuffer.wrap(s.getBytes(StandardCharsets.US_ASCII))))
    val httpStage = new Http1ServerStage(ServerTestRoutes(), None) {
      override def reset(): Unit = head.stageShutdown()     // shutdown the stage after a complete request
    }
    pipeline.LeafBuilder(httpStage).base(head)
    head.sendInboundCommand(Cmd.Connected)

    Await.result(head.result, 10000.milliseconds)
  }

  "Http4sStage" should {

    ServerTestRoutes.testRequestResults.zipWithIndex.foreach { case ((req, (status,headers,resp)), i) =>
      s"Run request $i Run request: --------\n${req.split("\r\n\r\n")(0)}\n" in {

        val result = runRequest(Seq(req))

//        println("Received ---------------------------\n" + makeString(result) + "\n----------------------------")

        val (sresult,hresult,body) = ResponseParser(result)

        status should equal(sresult)
        body should equal(resp)
        headers should equal(hresult)

      }
    }
  }

}
